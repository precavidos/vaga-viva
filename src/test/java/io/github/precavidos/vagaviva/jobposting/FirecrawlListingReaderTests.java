package io.github.precavidos.vagaviva.jobposting;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.sun.net.httpserver.HttpServer;

import io.github.precavidos.vagaviva.jobposting.ListingPage.PageLink;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** A career page's links through Firecrawl's {@code /v2/scrape}, against a local fake. */
@ExtendWith(OutputCaptureExtension.class)
class FirecrawlListingReaderTests {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final String URL = "https://jobs.lever.co/empresa-sigilosa";

	private HttpServer server;

	private volatile String lastBody;

	private volatile int status = 200;

	private volatile String responseBody;

	private FirecrawlListingReader reader;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/", exchange -> {
			lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		reader = new FirecrawlListingReader(new FirecrawlProperties("fc-test-key",
				URI.create("http://127.0.0.1:" + server.getAddress().getPort()), Duration.ofSeconds(5)));
	}

	@AfterEach
	void stopServer() {
		reader.close();
		server.stop(0);
	}

	@Test
	void asksForTheWholePageAsMarkdownAndLinks() {
		responseBody = answer("[Dev Java](https://jobs.lever.co/x/1)", "[\"https://jobs.lever.co/x/1\"]", 200);

		reader.read(URL);

		JsonNode request = JSON.readTree(lastBody);
		assertThat(request.path("url").asString()).isEqualTo(URL);
		assertThat(request.path("formats").toString()).isEqualTo("[\"markdown\",\"links\"]");
		assertThat(request.path("onlyMainContent").asBoolean()).isFalse();
	}

	@Test
	void givesTheMarkdownLinksWithTextThenTheOtherLinks() {
		responseBody = answer("[Apply](https://jobs.lever.co/x/1) [Dev Java](https://jobs.lever.co/x/1)",
				"[\"https://jobs.lever.co/x/1\", \"https://jobs.lever.co/x/2\"]", 200);

		assertThat(reader.read(URL)).hasValueSatisfying(page -> assertThat(page.links()).containsExactly(
				new PageLink("https://jobs.lever.co/x/1", "Apply"), new PageLink("https://jobs.lever.co/x/1", "Dev Java"),
				new PageLink("https://jobs.lever.co/x/2", "")));
	}

	@Test
	void givesAnEmptyListForAPageWithoutLinks() {
		responseBody = answer("Nenhuma vaga aberta no momento.", "[]", 200);

		assertThat(reader.read(URL)).hasValueSatisfying(page -> assertThat(page.links()).isEmpty());
	}

	@Test
	void givesNothingWhenThePageOrFirecrawlFailed() {
		responseBody = answer("Not found", "[]", 404);
		assertThat(reader.read(URL)).isEmpty();

		status = 402;
		responseBody = "{\"success\":false}";
		assertThat(reader.read(URL)).isEmpty();

		status = 200;
		responseBody = "not json";
		assertThat(reader.read(URL)).isEmpty();
	}

	@Test
	void neverLogsTheUrlOrTheKey(CapturedOutput log) {
		status = 500;
		responseBody = "{\"success\":false,\"error\":\"" + URL + "\"}";
		reader.read(URL);

		assertThat(log.getAll()).doesNotContain("empresa-sigilosa").doesNotContain("fc-test-key");
	}

	private static String answer(String markdown, String links, int pageStatus) {
		return """
				{"success":true,"data":{"markdown":%s,"links":%s,"metadata":{"sourceURL":"%s","statusCode":%d}}}"""
			.formatted(JSON.writeValueAsString(markdown), links, URL, pageStatus);
	}

}
