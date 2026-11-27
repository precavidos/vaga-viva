package io.github.precavidos.vagaviva.watch;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.precavidos.vagaviva.archetype.ArchetypeFit;
import io.github.precavidos.vagaviva.jobposting.ExtractedJobPosting;
import io.github.precavidos.vagaviva.jobposting.JobPostingExtractor;
import io.github.precavidos.vagaviva.jobposting.JobPostingReader;
import io.github.precavidos.vagaviva.jobposting.ListingPageReader;

/**
 * The Sunday check: reads each followed career page once, however many
 * students follow it, and compares its posting links with the ones already
 * seen. Up to {@value #MAX_READS_PER_PAGE} new postings per page are read; the
 * tech ones (at least one technology of the vocabulary) become alerts for
 * Monday's e-mail. Deterministic: no LLM (ADR 0002). The log holds counts,
 * never a URL (ADR 0003).
 */
@Service
class CompanyWatchCheck {

	private static final Logger log = LoggerFactory.getLogger(CompanyWatchCheck.class);

	/** New postings read per page and week; the rest are recorded as seen without an alert. */
	static final int MAX_READS_PER_PAGE = 10;

	/** Seen links are forgotten this long after they were last on the page. */
	static final Duration KEEP_SEEN = Duration.ofDays(90);

	private final CompanyWatchStore store;

	private final ListingPageReader listings;

	private final JobPostingReader postings;

	private final JobPostingExtractor extractor;

	private final Clock clock;

	CompanyWatchCheck(CompanyWatchStore store, ListingPageReader listings, JobPostingReader postings,
			JobPostingExtractor extractor, Clock clock) {
		this.store = store;
		this.listings = listings;
		this.postings = postings;
		this.extractor = extractor;
		this.clock = clock;
	}

	/**
	 * @param pages pages checked
	 * @param failed pages that could not be read
	 * @param newPostings postings not seen before
	 * @param alerts new tech postings, shown in Monday's e-mail
	 */
	record Summary(int pages, int failed, int newPostings, int alerts) {
	}

	/** Not transactional as a whole: each page and posting is saved as soon as it is known. */
	Summary run() {
		int purged = store.purgeSeen(clock.instant().minus(KEEP_SEEN));
		int pages = 0;
		int failed = 0;
		int fresh = 0;
		int alerts = 0;
		for (var page : store.followedPages()) {
			pages++;
			try {
				var result = check(page);
				if (result.isEmpty()) {
					failed++;
					store.checked(page.id(), false, clock.instant());
					continue;
				}
				fresh += result.get().newPostings();
				alerts += result.get().alerts();
				store.checked(page.id(), true, clock.instant());
			}
			catch (RuntimeException ex) {
				failed++;
				log.warn("Career page {} check failed: {}", page.id(), ex.getClass().getSimpleName());
				store.checked(page.id(), false, clock.instant());
			}
		}
		log.info("Company watch: {} pages, {} failed, {} new postings, {} alerts, {} old links purged", pages, failed,
				fresh, alerts, purged);
		return new Summary(pages, failed, fresh, alerts);
	}

	private record PageResult(int newPostings, int alerts) {
	}

	/** Empty when the page could not be read; its seen links stay as they were. */
	private Optional<PageResult> check(CompanyWatchStore.Page page) {
		var address = CareerPageAddress.parse(page.url());
		if (address.isEmpty()) {
			return Optional.empty();
		}
		var listing = listings.read(page.url());
		if (listing.isEmpty()) {
			return Optional.empty();
		}
		var seen = store.seenUrls(page.id());
		var listed = ListedPostings.of(address.get(), listing.get());
		if (listed.isEmpty() && !seen.isEmpty()) {
			// It had postings and now has none: most likely the page moved or changed its layout.
			return Optional.empty();
		}
		int fresh = 0;
		int reads = 0;
		int alerts = 0;
		// Page order: the platforms list the newest postings first.
		for (var posting : listed) {
			if (seen.contains(posting.url())) {
				store.seen(page.id(), posting.url(), clock.instant());
				continue;
			}
			fresh++;
			if (reads == MAX_READS_PER_PAGE) {
				store.seen(page.id(), posting.url(), clock.instant());
				continue;
			}
			reads++;
			var text = postings.read(posting.url());
			// An unreadable posting is recorded as seen too: tried again every week, it would take a read from newer ones.
			var extracted = text.map(extractor::extract);
			if (extracted.isEmpty() || extracted.get().recognition().technologies().isEmpty()) {
				store.seen(page.id(), posting.url(), clock.instant());
				continue;
			}
			// The title is the page's link text, never a line of the posting (ADR 0003).
			store.alert(page.id(), posting, archetype(extracted.get()), clock.instant());
			alerts++;
		}
		return Optional.of(new PageResult(fresh, alerts));
	}

	/** The archetype whose stack the posting names, or {@code null} when none of ours fits. */
	private static String archetype(ExtractedJobPosting posting) {
		var status = posting.archetypeFit().status();
		return status == ArchetypeFit.Status.COVERED || status == ArchetypeFit.Status.PARTIAL ? posting.archetype()
				: null;
	}

}
