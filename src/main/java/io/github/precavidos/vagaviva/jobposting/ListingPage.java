package io.github.precavidos.vagaviva.jobposting;

import java.util.List;

/**
 * A company's list of postings, as rendered: the links on the page, in page
 * order. Kept only in memory.
 *
 * @param links every link on the page, the ones with text first as they appear, then the others
 */
public record ListingPage(List<PageLink> links) {

	public ListingPage {
		links = List.copyOf(links);
	}

	/**
	 * @param url the link's target, as written on the page
	 * @param text the first line of the link's text, blank when the link has none
	 */
	public record PageLink(String url, String text) {
	}

}
