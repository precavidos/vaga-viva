package io.github.precavidos.vagaviva.watch;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import io.github.precavidos.vagaviva.jobposting.ListingPageReader;

/**
 * The switch of the company watch: on only with {@code COMPANY_WATCH_SECRET}
 * and Firecrawl both set. Off, the follow page and its API answer 404 and the
 * weekly e-mail stays as it was.
 */
@Component
@EnableConfigurationProperties(CompanyWatchProperties.class)
public class CompanyWatch {

	/** How many companies a student may follow. */
	public static final int MAX_FOLLOWS = 3;

	/** Failed weekly checks in a row after which the e-mail asks the student to check the link. */
	public static final int FAILURES_TO_WARN = 3;

	private final CompanyWatchProperties properties;

	private final ListingPageReader listings;

	CompanyWatch(CompanyWatchProperties properties, ListingPageReader listings) {
		this.properties = properties;
		this.listings = listings;
	}

	public boolean enabled() {
		return !properties.secret().isBlank() && listings.enabled();
	}

	String secret() {
		return properties.secret();
	}

	boolean listingsEnabled() {
		return listings.enabled();
	}

}
