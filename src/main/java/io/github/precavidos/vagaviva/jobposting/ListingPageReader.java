package io.github.precavidos.vagaviva.jobposting;

import java.util.Optional;

/**
 * Reads the list of postings on a company's career page. Those pages are
 * often built by JavaScript, so they are always rendered by a service. Without
 * one configured, {@link #NONE} answers and companies cannot be followed.
 */
public interface ListingPageReader {

	/** No rendering service. */
	ListingPageReader NONE = new ListingPageReader() {

		@Override
		public boolean enabled() {
			return false;
		}

		@Override
		public Optional<ListingPage> read(String url) {
			return Optional.empty();
		}

	};

	boolean enabled();

	/**
	 * The page's links, or empty when the page could not be read or answered
	 * with an error. Never throws, and never logs the URL (ADR 0003).
	 */
	Optional<ListingPage> read(String url);

}
