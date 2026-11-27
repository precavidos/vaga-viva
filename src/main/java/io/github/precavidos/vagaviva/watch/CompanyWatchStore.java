package io.github.precavidos.vagaviva.watch;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The tables of the company watch (V5). Follows hold the student's login;
 * pages and seen postings are public addresses with no student data.
 */
@Repository
class CompanyWatchStore {

	private final JdbcClient jdbc;

	CompanyWatchStore(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** A company a student follows. */
	record Follow(long pageId, String url, String name, int consecutiveFailures) {
	}

	/** A page someone follows, for the weekly check. */
	record Page(long id, String url) {
	}

	/** A tech posting the weekly check found on a page the student follows. */
	record NewPosting(long pageId, String company, String url, String title, String archetype) {
	}

	/** Whether the student gets the weekly e-mail (owned by the e-mail feature, V4). */
	boolean subscribed(String login) {
		return jdbc.sql("select count(*) from weekly_email_subscription where student_login = :login")
			.param("login", login)
			.query(Long.class)
			.single() > 0;
	}

	List<Follow> follows(String login) {
		return jdbc.sql("""
				select p.id, p.url, f.name, p.consecutive_failures from company_follow f
				join career_page p on p.id = f.career_page_id
				where f.student_login = :login order by f.created_at, p.id""")
			.param("login", login)
			.query((rs, n) -> new Follow(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4)))
			.list();
	}

	Optional<Long> pageId(String url) {
		return jdbc.sql("select id from career_page where url = :url").param("url", url).query(Long.class).optional();
	}

	/**
	 * Follows the page, creating it when nobody followed it yet; a new page
	 * records {@code current} as already seen, so they never become alerts.
	 * An existing page keeps its own record: the weekly check compares against
	 * it, and a follower who came first must still hear about what is new.
	 */
	@Transactional
	long follow(String login, String url, String name, Collection<ListedPosting> current, Instant now) {
		Optional<Long> existing = pageId(url);
		long id = existing.orElseGet(() -> jdbc.sql("""
				insert into career_page (url, created_at) values (:url, :now)
				on conflict (url) do update set url = excluded.url returning id""")
			.param("url", url)
			.param("now", timestamp(now))
			.query(Long.class)
			.single());
		jdbc.sql("""
				insert into company_follow (student_login, career_page_id, name, created_at)
				values (:login, :id, :name, :now) on conflict (student_login, career_page_id) do update set name = excluded.name""")
			.param("login", login)
			.param("id", id)
			.param("name", name)
			.param("now", timestamp(now))
			.update();
		if (existing.isEmpty()) {
			for (ListedPosting posting : current) {
				seen(id, posting.url(), now);
			}
		}
		return id;
	}

	boolean rename(String login, long pageId, String name) {
		return jdbc.sql("update company_follow set name = :name where student_login = :login and career_page_id = :id")
			.param("name", name)
			.param("login", login)
			.param("id", pageId)
			.update() > 0;
	}

	/** The page, and its seen links, go too when nobody else follows it (trigger in V5). */
	boolean unfollow(String login, long pageId) {
		return jdbc.sql("delete from company_follow where student_login = :login and career_page_id = :id")
			.param("login", login)
			.param("id", pageId)
			.update() > 0;
	}

	List<Page> followedPages() {
		return jdbc.sql("""
				select p.id, p.url from career_page p
				where exists (select 1 from company_follow f where f.career_page_id = p.id) order by p.id""")
			.query((rs, n) -> new Page(rs.getLong(1), rs.getString(2)))
			.list();
	}

	Set<String> seenUrls(long pageId) {
		return new HashSet<>(jdbc.sql("select url from seen_posting where career_page_id = :id")
			.param("id", pageId)
			.query(String.class)
			.list());
	}

	/** Records a link as seen now, without an alert; a link already seen only moves its last-seen time. */
	void seen(long pageId, String url, Instant now) {
		jdbc.sql("""
				insert into seen_posting (career_page_id, url, first_seen_at, last_seen_at) values (:id, :url, :now, :now)
				on conflict (career_page_id, url) do update set last_seen_at = excluded.last_seen_at""")
			.param("id", pageId)
			.param("url", url)
			.param("now", timestamp(now))
			.update();
	}

	/** Records a new tech posting, which the next weekly e-mail shows. */
	void alert(long pageId, ListedPosting posting, String archetype, Instant now) {
		jdbc.sql("""
				insert into seen_posting (career_page_id, url, title, alert, archetype, first_seen_at, last_seen_at)
				values (:id, :url, :title, true, :archetype, :now, :now)
				on conflict (career_page_id, url) do update set last_seen_at = excluded.last_seen_at""")
			.param("id", pageId)
			.param("url", posting.url())
			.param("title", posting.title())
			.param("archetype", archetype)
			.param("now", timestamp(now))
			.update();
	}

	void checked(long pageId, boolean read, Instant now) {
		jdbc.sql(read ? "update career_page set consecutive_failures = 0, last_checked_at = :now where id = :id"
				: "update career_page set consecutive_failures = consecutive_failures + 1, last_checked_at = :now where id = :id")
			.param("id", pageId)
			.param("now", timestamp(now))
			.update();
	}

	/**
	 * Forgets links not seen since {@code before}, on pages whose last check
	 * worked: a page that keeps failing keeps its links, so its open postings
	 * do not all come back as new when it is read again.
	 */
	int purgeSeen(Instant before) {
		return jdbc.sql("""
				delete from seen_posting s using career_page p
				where s.career_page_id = p.id and p.consecutive_failures = 0 and s.last_seen_at < :before""")
			.param("before", timestamp(before))
			.update();
	}

	/** The tech postings found after {@code since} and after the student started following each page. */
	List<NewPosting> newPostings(String login, Instant since) {
		return jdbc.sql("""
				select f.career_page_id, f.name, s.url, s.title, s.archetype from company_follow f
				join seen_posting s on s.career_page_id = f.career_page_id
				where f.student_login = :login and s.alert and s.first_seen_at > :since and s.first_seen_at > f.created_at
				order by s.first_seen_at desc, s.url""")
			.param("login", login)
			.param("since", timestamp(since))
			.query((rs, n) -> new NewPosting(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
					rs.getString(5)))
			.list();
	}

	private static Timestamp timestamp(Instant instant) {
		return Timestamp.from(instant);
	}

}
