package io.github.precavidos.vagaviva.jobposting;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Renders a career page with Firecrawl's {@code /v2/scrape} and keeps its
 * main content as text. Firecrawl downloads the page from its own servers, so
 * the app's network is never reached through it; the URL has already passed
 * {@link JobPostingFetcher}'s checks before this runs. Firecrawl receives only
 * the public URL of the posting, never anything about the student, and
 * nothing here logs the URL or the page (ADR 0003).
 */
class FirecrawlPageReader implements RenderedPageReader, AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(FirecrawlPageReader.class);

	private static final JsonMapper JSON = JsonMapper.builder().build();

	/** Largest answer read; the markdown of a career page is far smaller. */
	static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;

	/** A posting changes slowly: a copy Firecrawl read in the last day is fresh enough and faster. */
	static final Duration MAX_AGE = Duration.ofDays(1);

	private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*\\]\\([^)]*\\)");

	private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)\\]\\([^)]*\\)");

	private static final Pattern BARE_URL = Pattern.compile("<?https?://\\S+>?");

	private final HttpClient client;

	private final URI scrape;

	private final String apiKey;

	private final Duration timeout;

	FirecrawlPageReader(FirecrawlProperties properties) {
		this.client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.followRedirects(HttpClient.Redirect.NEVER)
			.build();
		this.scrape = properties.baseUrl().resolve("/v2/scrape");
		this.apiKey = properties.apiKey();
		this.timeout = properties.timeout();
	}

	@Override
	public Optional<String> read(String url) {
		try {
			HttpResponse<InputStream> response = client.send(request(url), HttpResponse.BodyHandlers.ofInputStream());
			try (InputStream body = response.body()) {
				if (response.statusCode() != 200) {
					log.warn("Firecrawl answered {}", response.statusCode());
					return Optional.empty();
				}
				byte[] bytes = body.readNBytes(MAX_RESPONSE_BYTES + 1);
				if (bytes.length > MAX_RESPONSE_BYTES) {
					log.warn("Firecrawl answer too large");
					return Optional.empty();
				}
				return text(JSON.readTree(bytes));
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
		catch (IOException | RuntimeException ex) {
			log.warn("Firecrawl call failed: {}", ex.getClass().getName());
			return Optional.empty();
		}
	}

	private HttpRequest request(String url) {
		long timeoutMs = timeout.toMillis();
		// Firecrawl's own deadline is a little shorter, so it answers before the call is cut.
		String body = JSON.writeValueAsString(Map.of("url", url.strip(), "formats", List.of("markdown"),
				"onlyMainContent", true, "timeout", Math.max(1_000, timeoutMs - 3_000), "maxAge", MAX_AGE.toMillis()));
		return HttpRequest.newBuilder(scrape)
			.timeout(timeout)
			.header("Authorization", "Bearer " + apiKey)
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
			.build();
	}

	private static Optional<String> text(JsonNode answer) {
		JsonNode data = answer.path("data");
		if (!answer.path("success").asBoolean(false) || !data.path("markdown").isString()) {
			return Optional.empty();
		}
		int pageStatus = data.path("metadata").path("statusCode").asInt(200);
		if (pageStatus < 200 || pageStatus >= 300) {
			return Optional.empty();
		}
		String text = plain(data.path("markdown").asString());
		if (text.length() < JobPostingPageReader.MIN_TEXT_LENGTH) {
			return Optional.empty();
		}
		return Optional.of(JobPostingPageReader.truncate(text));
	}

	/**
	 * The markdown without images, link targets and bare URLs: an address such
	 * as {@code /careers/java-team} would otherwise count as a skill the posting
	 * never asks for.
	 */
	static String plain(String markdown) {
		String text = IMAGE.matcher(markdown).replaceAll("");
		text = LINK.matcher(text).replaceAll("$1");
		text = BARE_URL.matcher(text).replaceAll("");
		return text.strip();
	}

	@Override
	public void close() {
		client.close();
	}

}
