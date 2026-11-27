package io.github.precavidos.vagaviva.jobposting;

import java.util.List;

import io.github.precavidos.vagaviva.jobposting.ListingPage.PageLink;

/** Lets tests of other packages read a saved page the way {@link FirecrawlListingReader} does. */
public final class TestListings {

	private TestListings() {
	}

	public static List<PageLink> markdownLinks(String markdown) {
		return MarkdownLinks.parse(markdown);
	}

}
