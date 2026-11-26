package io.github.precavidos.vagaviva.jobposting;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Firecrawl settings. A blank {@code apiKey} turns the rendered reading off,
 * and pages built by JavaScript stay unreadable, as before. {@code timeout}
 * covers the whole call, rendering included.
 */
@ConfigurationProperties("vagaviva.firecrawl")
record FirecrawlProperties(@DefaultValue("") String apiKey,
		@DefaultValue("https://api.firecrawl.dev") URI baseUrl,
		@DefaultValue("25s") Duration timeout) {

	/** Never shows the API key. */
	@Override
	public String toString() {
		return "FirecrawlProperties[apiKey=" + (apiKey.isBlank() ? "<blank>" : "****") + ", baseUrl=" + baseUrl
				+ ", timeout=" + timeout + "]";
	}

}
