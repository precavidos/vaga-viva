package io.github.precavidos.vagaviva.watch;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.github.precavidos.vagaviva.jobposting.ListingPage;
import io.github.precavidos.vagaviva.jobposting.ListingPage.PageLink;

/** The postings on a career page: each one once, in page order, with the best title the page gives it. */
final class ListedPostings {

	/** Button texts, never a posting's title. Compared without accents and case. */
	private static final Set<String> BUTTONS = Set.of("apply", "apply now", "apply for this job", "aplicar",
			"candidatar", "candidatar-se", "candidate-se", "quero me candidatar", "ver vaga", "ver detalhes",
			"saiba mais", "ver mais", "detalhes", "learn more", "view job", "see details", "read more", "more");

	private ListedPostings() {
	}

	static List<ListedPosting> of(CareerPageAddress page, ListingPage listing) {
		var titles = new LinkedHashMap<String, String>();
		for (PageLink link : listing.links()) {
			var posting = page.posting(link.url());
			if (posting.isEmpty()) {
				continue;
			}
			var title = isTitle(link.text()) ? link.text().strip() : "";
			titles.merge(posting.get(), title, (old, candidate) -> old.isEmpty() ? candidate : old);
		}
		var postings = new ArrayList<ListedPosting>();
		titles.forEach((url, title) -> postings.add(new ListedPosting(url, title)));
		return postings;
	}

	private static boolean isTitle(String text) {
		if (text == null || text.isBlank()) {
			return false;
		}
		var plain = Normalizer.normalize(text.strip(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT);
		return !BUTTONS.contains(plain);
	}

}
