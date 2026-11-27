package io.github.precavidos.vagaviva.watch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;
import io.github.precavidos.vagaviva.jobposting.JobPostingReader;
import io.github.precavidos.vagaviva.jobposting.ListingPage;
import io.github.precavidos.vagaviva.jobposting.ListingPage.PageLink;
import io.github.precavidos.vagaviva.jobposting.ListingPageReader;

/**
 * Following companies, the Sunday check and what Monday's e-mail gets, against
 * Postgres. Firecrawl and the posting reader are fakes. The secret comes from
 * its environment variable, as in production.
 */
@Import({ TestcontainersConfiguration.class, CompanyWatchApiTests.TestClock.class })
@SpringBootTest(properties = "COMPANY_WATCH_SECRET=watch-secret")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class CompanyWatchApiTests {

	private static final String PAGE = "https://compass.gupy.io/";

	private static final String JAVA_POSTING = "Pessoa Desenvolvedora Java Júnior. Vamos construir APIs com Java e "
			+ "Spring Boot, gravando dados em PostgreSQL, com testes automatizados e revisão de código em equipe. "
			+ "Buscamos quem goste de aprender e de colaborar com o time todos os dias.";

	private static final String PYTHON_POSTING = "Pessoa Desenvolvedora Python. Vamos construir serviços com Python "
			+ "e Django, com testes automatizados e revisão de código em equipe, para um produto usado no Brasil "
			+ "inteiro. Buscamos quem goste de aprender e de colaborar com o time.";

	private static final String SALES_POSTING = "Executivo de Contas. Você vai prospectar clientes, negociar "
			+ "contratos e acompanhar a carteira de clientes da região Sul, com metas mensais e viagens frequentes. "
			+ "Buscamos experiência comercial e boa comunicação com clientes.";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	ClientRegistrationRepository registrations;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	CompanyNews news;

	@Autowired
	MutableClock clock;

	@MockitoBean
	ListingPageReader listings;

	@MockitoBean
	JobPostingReader postings;

	@TestConfiguration(proxyBeanMethods = false)
	static class TestClock {

		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock(Instant.parse("2026-11-22T23:00:00Z"));
		}

	}

	@BeforeEach
	void clean() {
		jdbc.sql("delete from company_follow").update();
		jdbc.sql("delete from career_page").update();
		jdbc.sql("delete from weekly_email_subscription").update();
		clock.set(Instant.parse("2026-11-22T23:00:00Z"));
		when(listings.enabled()).thenReturn(true);
	}

	@Test
	void onlyWeeklyEmailSubscribersCanFollow() {
		var response = follow("maria", PAGE, null);

		assertThat(response).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(response).bodyJson().extractingPath("$.detail").asString().contains("e-mail semanal");
		assertThat(mvc.get().uri("/api/company-follows").with(student("maria")).exchange()).hasStatus(HttpStatus.OK)
			.bodyJson()
			.extractingPath("$.subscribed")
			.isEqualTo(false);
	}

	@Test
	void refusesPagesOutsideTheKnownPlatforms() {
		subscribe("maria");

		var response = follow("maria", "https://www.catho.com.br/vagas/", null);

		assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(response).bodyJson().extractingPath("$.detail").asString().contains("Gupy", "Workday");
		verify(listings, never()).read(anyString());
	}

	@Test
	void followingShowsTheOpenPostingsAndRecordsThemAsSeen() {
		subscribe("maria");
		listing(PAGE, "1", "2");

		var response = follow("maria", "https://Compass.gupy.io/?utm=x", null);

		assertThat(response).hasStatus(HttpStatus.CREATED);
		assertThat(response).bodyJson().extractingPath("$.count").isEqualTo(2);
		assertThat(response).bodyJson().extractingPath("$.name").isEqualTo("Compass");
		assertThat(response).bodyJson().extractingPath("$.postings[0].url").isEqualTo("https://compass.gupy.io/jobs/1");
		assertThat(response).bodyJson().extractingPath("$.postings[0].title").isEqualTo("Vaga 1");
		assertThat(seenCount()).isEqualTo(2);
		assertThat(mvc.get().uri("/api/company-follows").with(student("maria")).exchange()).bodyJson()
			.extractingPath("$.follows[0].url")
			.isEqualTo(PAGE);
	}

	@Test
	void followsAtMostThreeCompaniesAndEachOnce() {
		subscribe("maria");
		for (String company : List.of("a", "b", "c", "d")) {
			listing("https://" + company + ".gupy.io/");
		}
		for (String company : List.of("a", "b", "c")) {
			assertThat(follow("maria", "https://" + company + ".gupy.io/", null)).hasStatus(HttpStatus.CREATED);
		}

		assertThat(follow("maria", "https://a.gupy.io/", null)).hasStatus(HttpStatus.CONFLICT)
			.bodyJson()
			.extractingPath("$.detail")
			.asString()
			.contains("já segue esta empresa");
		assertThat(follow("maria", "https://d.gupy.io/", null)).hasStatus(HttpStatus.CONFLICT)
			.bodyJson()
			.extractingPath("$.detail")
			.asString()
			.contains("3 empresas");
	}

	@Test
	void anUnreadablePageIsNotFollowed() {
		subscribe("maria");
		when(listings.read(PAGE)).thenReturn(Optional.empty());

		assertThat(follow("maria", PAGE, null)).hasStatus(HttpStatus.BAD_GATEWAY);
		assertThat(pageCount()).isZero();
	}

	@Test
	void theStudentRenamesAndUnfollowsAndAnUnfollowedPageGoes() {
		subscribe("maria");
		listing(PAGE, "1");
		long id = followId("maria", PAGE, "Compass UOL");

		assertThat(mvc.patch()
			.uri("/api/company-follows/" + id)
			.with(student("maria"))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"name\":\"Compass\"}")
			.exchange()).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.delete().uri("/api/company-follows/" + id).with(student("joao")).with(csrf()).exchange())
			.hasStatus(HttpStatus.NOT_FOUND);
		assertThat(jdbc.sql("select name from company_follow").query(String.class).single()).isEqualTo("Compass");

		assertThat(mvc.delete().uri("/api/company-follows/" + id).with(student("maria")).with(csrf()).exchange())
			.hasStatus(HttpStatus.NO_CONTENT);
		assertThat(pageCount()).isZero();
		assertThat(seenCount()).isZero();
	}

	@Test
	void aPageStaysWhileSomeoneStillFollowsIt() {
		subscribe("maria");
		subscribe("joao");
		listing(PAGE, "1");
		long id = followId("maria", PAGE, null);
		followId("joao", PAGE, null);

		mvc.delete().uri("/api/company-follows/" + id).with(student("maria")).with(csrf()).exchange();

		assertThat(pageCount()).isEqualTo(1);
		assertThat(seenCount()).isEqualTo(1);
	}

	@Test
	void unsubscribingFromTheWeeklyEmailDeletesTheFollowsAndTheirPages() {
		subscribe("maria");
		listing(PAGE, "1");
		followId("maria", PAGE, null);

		assertThat(mvc.post()
			.uri("/api/weekly-email/unsubscribe")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"tok-maria\"}")
			.exchange()).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(jdbc.sql("select count(*) from company_follow").query(Long.class).single()).isZero();
		assertThat(pageCount()).isZero();
		assertThat(seenCount()).isZero();
	}

	@Test
	void theCheckNeedsTheSecret() {
		assertThat(mvc.post().uri(CompanyWatchCheckController.PATH).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.post().uri(CompanyWatchCheckController.PATH).header("X-Vaga-Viva-Secret", "wrong").exchange())
			.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void theCheckAlertsOnlyNewTechPostingsAndMondaysEmailGetsThem() {
		subscribe("maria");
		listing(PAGE, "1");
		followId("maria", PAGE, null);
		clock.advance(Duration.ofDays(7));
		listing(PAGE, "1", "2", "3", "4");
		when(postings.read("https://compass.gupy.io/jobs/2")).thenReturn(Optional.of(PYTHON_POSTING));
		when(postings.read("https://compass.gupy.io/jobs/3")).thenReturn(Optional.of(JAVA_POSTING));
		when(postings.read("https://compass.gupy.io/jobs/4")).thenReturn(Optional.of(SALES_POSTING));

		var response = check();

		assertThat(response).hasStatus(HttpStatus.OK);
		assertThat(response).bodyJson().extractingPath("$.pages").isEqualTo(1);
		assertThat(response).bodyJson().extractingPath("$.newPostings").isEqualTo(3);
		assertThat(response).bodyJson().extractingPath("$.alerts").isEqualTo(2);
		verify(postings, never()).read("https://compass.gupy.io/jobs/1");

		clock.advance(Duration.ofHours(12));
		var section = news.forStudent("maria", "java-spring").orElseThrow();
		assertThat(section.following()).isTrue();
		assertThat(section.postings()).extracting(CompanyNews.Posting::url)
			.containsExactly("https://compass.gupy.io/jobs/3", "https://compass.gupy.io/jobs/2");
		assertThat(section.postings().get(0).matchesPlan()).isTrue();
		assertThat(section.postings().get(0).company()).isEqualTo("Compass");
		assertThat(section.postings().get(1).matchesPlan()).isFalse();

		// The next week those postings are old news, and nothing is read again.
		clock.advance(Duration.ofDays(7));
		check();
		verify(postings, times(1)).read("https://compass.gupy.io/jobs/3");
		assertThat(news.forStudent("maria", "java-spring").orElseThrow().postings()).isEmpty();
	}

	@Test
	void readsEachPageOnceAndAtMostTenNewPostings() {
		subscribe("maria");
		subscribe("joao");
		listing(PAGE);
		followId("maria", PAGE, null);
		followId("joao", PAGE, null);
		var ids = IntStream.rangeClosed(1, 12).mapToObj(String::valueOf).toArray(String[]::new);
		listing(PAGE, ids);
		when(postings.read(anyString())).thenReturn(Optional.of(JAVA_POSTING));
		clock.advance(Duration.ofDays(7));
		clearInvocations(listings);

		var response = check();

		assertThat(response).bodyJson().extractingPath("$.newPostings").isEqualTo(12);
		assertThat(response).bodyJson().extractingPath("$.alerts").isEqualTo(10);
		verify(listings, times(1)).read(PAGE);
		verify(postings, times(10)).read(anyString());
		verify(postings, never()).read("https://compass.gupy.io/jobs/11");
		assertThat(seenCount()).isEqualTo(12);
	}

	@Test
	void countsFailedChecksInARowAndWarnsOnTheThird() {
		subscribe("maria");
		listing(PAGE, "1");
		followId("maria", PAGE, null);
		when(listings.read(PAGE)).thenReturn(Optional.empty());

		for (int week = 1; week <= 3; week++) {
			assertThat(check()).bodyJson().extractingPath("$.failed").isEqualTo(1);
			assertThat(news.forStudent("maria", "java-spring").orElseThrow().checkLinks())
				.hasSize(week < 3 ? 0 : 1);
		}
		assertThat(mvc.get().uri("/api/company-follows").with(student("maria")).exchange()).bodyJson()
			.extractingPath("$.follows[0].checkLink")
			.isEqualTo(true);
		assertThat(pageCount()).as("never removed automatically").isEqualTo(1);

		listing(PAGE, "1");
		check();
		assertThat(news.forStudent("maria", "java-spring").orElseThrow().checkLinks()).isEmpty();
	}

	@Test
	void anUnreadablePostingIsNotTriedAgain() {
		subscribe("maria");
		listing(PAGE, "1");
		followId("maria", PAGE, null);
		listing(PAGE, "1", "2");
		when(postings.read(anyString())).thenReturn(Optional.empty());

		check();
		clock.advance(Duration.ofDays(7));
		check();

		verify(postings, times(1)).read("https://compass.gupy.io/jobs/2");
	}

	@Test
	void aPageThatSuddenlyListsNothingCountsAsAFailure() {
		subscribe("maria");
		listing(PAGE, "1");
		followId("maria", PAGE, null);
		listing(PAGE);

		assertThat(check()).bodyJson().extractingPath("$.failed").isEqualTo(1);
	}

	@Test
	void aFailingPageKeepsItsLinks() {
		subscribe("maria");
		listing(PAGE, "1");
		followId("maria", PAGE, null);
		when(listings.read(PAGE)).thenReturn(Optional.empty());

		for (int week = 1; week <= 14; week++) {
			clock.advance(Duration.ofDays(7));
			check();
		}

		assertThat(seenCount()).isEqualTo(1);
	}

	@Test
	void forgetsLinksNinetyDaysAfterTheyWereLastSeen() {
		subscribe("maria");
		listing(PAGE, "1", "2");
		followId("maria", PAGE, null);
		listing(PAGE, "2");
		when(postings.read(anyString())).thenReturn(Optional.empty());

		// Thirteen weekly checks: jobs/2 is still on the page each time, jobs/1 left right after the follow.
		for (int week = 1; week <= 12; week++) {
			clock.advance(Duration.ofDays(7));
			check();
		}
		assertThat(seenCount()).isEqualTo(2);
		clock.advance(Duration.ofDays(7));
		check();

		assertThat(jdbc.sql("select url from seen_posting").query(String.class).list())
			.containsExactly("https://compass.gupy.io/jobs/2");
	}

	@Test
	void readsTheTextOfAKnownPostingOnly() {
		when(postings.read("https://compass.gupy.io/jobs/3")).thenReturn(Optional.of(JAVA_POSTING));

		assertThat(postingText("https://compass.gupy.io/jobs/3")).hasStatus(HttpStatus.OK)
			.bodyJson()
			.extractingPath("$.text")
			.isEqualTo(JAVA_POSTING);
		assertThat(postingText("https://www.catho.com.br/vagas/123")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(postingText("https://compass.gupy.io/jobs/4")).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		verify(postings, never()).read("https://www.catho.com.br/vagas/123");
	}

	@Test
	void theFollowPageIsOpen() {
		assertThat(mvc.get().uri("/empresas").exchange()).hasStatus(HttpStatus.OK);
	}

	@Test
	void neverLogsAPageOrPostingUrl(CapturedOutput log) {
		subscribe("maria");
		listing("https://empresa-sigilosa.gupy.io/", "987654");
		followId("maria", "https://empresa-sigilosa.gupy.io/", null);
		clock.advance(Duration.ofDays(7));
		listing("https://empresa-sigilosa.gupy.io/", "987654", "123456");
		check();

		assertThat(log.getAll()).doesNotContain("empresa-sigilosa").doesNotContain("987654").doesNotContain("123456");
	}

	private void subscribe(String login) {
		jdbc.sql("""
				insert into weekly_email_subscription (student_login, email, repository_full_name, consented_at, unsubscribe_token)
				values (:login, :email, :repo, :now, :token)""")
			.param("login", login)
			.param("email", login + "@example.com")
			.param("repo", login + "/vaga-viva-x")
			.param("now", Timestamp.from(clock.instant()))
			.param("token", "tok-" + login)
			.update();
	}

	private void listing(String page, String... ids) {
		var links = new ArrayList<PageLink>();
		links.add(new PageLink("https://www.gupy.io/", "Gupy"));
		for (String id : ids) {
			links.add(new PageLink("/jobs/" + id + "?jobBoardSource=gupy_public_page", "Vaga " + id));
		}
		when(listings.read(page)).thenReturn(Optional.of(new ListingPage(links)));
	}

	private org.springframework.test.web.servlet.assertj.MvcTestResult follow(String login, String url, String name) {
		var body = name == null ? "{\"url\":\"" + url + "\"}" : "{\"url\":\"" + url + "\",\"name\":\"" + name + "\"}";
		return mvc.post()
			.uri("/api/company-follows")
			.with(student(login))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();
	}

	private long followId(String login, String url, String name) {
		var response = follow(login, url, name);
		assertThat(response).hasStatus(HttpStatus.CREATED);
		return jdbc.sql("select id from career_page where url = :url")
			.param("url", CareerPageAddress.parse(url).orElseThrow().url())
			.query(Long.class)
			.single();
	}

	private org.springframework.test.web.servlet.assertj.MvcTestResult check() {
		return mvc.post().uri(CompanyWatchCheckController.PATH).header("X-Vaga-Viva-Secret", "watch-secret").exchange();
	}

	private org.springframework.test.web.servlet.assertj.MvcTestResult postingText(String url) {
		return mvc.post()
			.uri(PostingTextController.PATH)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"url\":\"" + url + "\"}")
			.exchange();
	}

	private long pageCount() {
		return jdbc.sql("select count(*) from career_page").query(Long.class).single();
	}

	private long seenCount() {
		return jdbc.sql("select count(*) from seen_posting").query(Long.class).single();
	}

	private OAuth2LoginRequestPostProcessor student(String login) {
		return oauth2Login().clientRegistration(registrations.findByRegistrationId("github"))
			.attributes((attributes) -> {
				attributes.put("id", 42);
				attributes.put("login", login);
			});
	}

	/** A clock the tests move by hand. */
	static class MutableClock extends Clock {

		private volatile Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void set(Instant instant) {
			now = instant;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

}
