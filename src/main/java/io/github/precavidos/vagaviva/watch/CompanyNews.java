package io.github.precavidos.vagaviva.watch;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * What the weekly e-mail tells a student about the companies they follow:
 * the tech postings found since last week, the ones matching their plan's
 * archetype first, and the pages that failed too many checks in a row.
 */
@Component
public class CompanyNews {

	/** Most postings in one e-mail. */
	static final int MAX_POSTINGS = 10;

	/** The Sunday check runs the night before the Monday e-mail, once a week. */
	static final Duration WINDOW = Duration.ofDays(7);

	private final CompanyWatch watch;

	private final CompanyWatchStore store;

	private final Clock clock;

	CompanyNews(CompanyWatch watch, CompanyWatchStore store, Clock clock) {
		this.watch = watch;
		this.store = store;
		this.clock = clock;
	}

	/**
	 * @param postings the new tech postings, matching ones first, at most {@value #MAX_POSTINGS}
	 * @param checkLinks names of followed companies whose page failed {@value CompanyWatch#FAILURES_TO_WARN}
	 * or more checks in a row
	 * @param following whether the student follows any company
	 */
	public record Section(List<Posting> postings, List<String> checkLinks, boolean following) {

		public Section {
			postings = List.copyOf(postings);
			checkLinks = List.copyOf(checkLinks);
		}

	}

	/** @param matchesPlan whether the posting names the stack of the student's plan */
	public record Posting(String title, String company, String url, boolean matchesPlan) {
	}

	/** Empty while the company watch is off: the e-mail stays as it was. */
	public Optional<Section> forStudent(String login, String planArchetype) {
		if (!watch.enabled()) {
			return Optional.empty();
		}
		var follows = store.follows(login);
		var checkLinks = follows.stream()
			.filter(f -> f.consecutiveFailures() >= CompanyWatch.FAILURES_TO_WARN)
			.map(CompanyWatchStore.Follow::name)
			.toList();
		var postings = follows.isEmpty() ? List.<Posting>of()
				: store.newPostings(login, clock.instant().minus(WINDOW))
					.stream()
					.map(p -> new Posting(p.title().isBlank() ? "Vaga em " + p.company() : p.title(), p.company(),
							p.url(), p.archetype() != null && p.archetype().equals(planArchetype)))
					// Stable: within each group the newest stay first.
					.sorted(Comparator.comparing((Posting p) -> !p.matchesPlan()))
					.limit(MAX_POSTINGS)
					.toList();
		return Optional.of(new Section(postings, checkLinks, !follows.isEmpty()));
	}

}
