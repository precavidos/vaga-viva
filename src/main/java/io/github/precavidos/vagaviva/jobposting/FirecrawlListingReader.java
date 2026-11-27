package io.github.precavidos.vagaviva.jobposting;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.precavidos.vagaviva.jobposting.ListingPage.PageLink;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a career page's links with one Firecrawl {@code /v2/scrape} (one
 * credit): the markdown gives each posting link with its title, and the
 * {@code links} list adds any link the markdown left out. Firecrawl receives
 * only the page's public URL, and nothing here logs it (ADR 0003).
 */
class FirecrawlListingReader implements ListingPageReader, AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(FirecrawlListingReader.class);

	private static final JsonMapper JSON = JsonMapper.builder().build();

	/** A copy Firecrawl read in the last hour is fresh enough for a weekly check. */
	static final Duration MAX_AGE = Duration.ofHours(1);

	private final HttpClient client;

	private final URI scrape;

	private final String apiKey;

	private final Duration timeout;

	FirecrawlListingReader(FirecrawlProperties properties) {
		this.client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.followRedirects(HttpClient.Redirect.NEVER)
			.build();
		this.scrape = properties.baseUrl().resolve("/v2/scrape");
		this.apiKey = properties.apiKey();
		this.timeout = properties.timeout();
	}

	@Override
	public boolean enabled() {
		return true;
	}

	@Override
	public Optional<ListingPage> read(String url) {
		try {
			HttpResponse<InputStream> response = client.send(request(url), HttpResponse.BodyHandlers.ofInputStream());
			try (InputStream body = response.body()) {
				if (response.statusCode() != 200) {
					log.warn("Firecrawl answered {} for a career page", response.statusCode());
					return Optional.empty();
				}
				byte[] bytes = body.readNBytes(FirecrawlPageReader.MAX_RESPONSE_BYTES + 1);
				if (bytes.length > FirecrawlPageReader.MAX_RESPONSE_BYTES) {
					log.warn("Firecrawl answer too large for a career page");
					return Optional.empty();
				}
				return listing(JSON.readTree(bytes));
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
		catch (IOException | RuntimeException ex) {
			log.warn("Firecrawl call for a career page failed: {}", ex.getClass().getName());
			return Optional.empty();
		}
	}

	private HttpRequest request(String url) {
		long timeoutMs = timeout.toMillis();
		String body = JSON.writeValueAsString(Map.of("url", url.strip(), "formats", List.of("markdown", "links"),
				"onlyMainContent", false, "timeout", Math.max(1_000, timeoutMs - 3_000), "maxAge",
				MAX_AGE.toMillis()));
		return HttpRequest.newBuilder(scrape)
			.timeout(timeout)
			.header("Authorization", "Bearer " + apiKey)
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
			.build();
	}

	private static Optional<ListingPage> listing(JsonNode answer) {
		JsonNode data = answer.path("data");
		if (!answer.path("success").asBoolean(false) || !data.isObject()) {
			return Optional.empty();
		}
		int pageStatus = data.path("metadata").path("statusCode").asInt(200);
		if (pageStatus < 200 || pageStatus >= 300) {
			return Optional.empty();
		}
		var links = new ArrayList<PageLink>();
		var seen = new LinkedHashSet<String>();
		if (data.path("markdown").isString()) {
			for (PageLink link : MarkdownLinks.parse(data.path("markdown").asString())) {
				seen.add(link.url());
				links.add(link);
			}
		}
		for (JsonNode link : data.path("links")) {
			if (link.isString() && seen.add(link.asString())) {
				links.add(new PageLink(link.asString(), ""));
			}
		}
		return Optional.of(new ListingPage(links));
	}

	@Override
	public void close() {
		client.close();
	}

}
