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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Firecrawl's {@code /v2/scrape}, against a local fake. */
@ExtendWith(OutputCaptureExtension.class)
class FirecrawlPageReaderTests {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final String URL = "https://vagas.exemplo.test/vaga-sigilosa-123";

	private static final String POSTING = "# Pessoa Desenvolvedora Back-end Júnior\n\n"
			+ "Vamos construir APIs com Java e Spring Boot, gravando os dados em PostgreSQL. "
			+ "Você vai escrever testes automatizados, revisar código com o time e aprender com pessoas mais experientes.";

	private HttpServer server;

	private volatile String lastPath;

	private volatile String lastAuthorization;

	private volatile String lastBody;

	private volatile int status = 200;

	private volatile String responseBody;

	private volatile long delayMs;

	private FirecrawlPageReader reader;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/", exchange -> {
			lastPath = exchange.getRequestURI().getPath();
			lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
			lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			try {
				Thread.sleep(delayMs);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			try {
				exchange.sendResponseHeaders(status, body.length);
				exchange.getResponseBody().write(body);
			}
			catch (IOException ex) {
				// The reader gave up first.
			}
			exchange.close();
		});
		server.start();
		reader = reader(Duration.ofSeconds(5));
	}

	@AfterEach
	void stopServer() {
		reader.close();
		server.stop(0);
	}

	@Test
	void asksForTheMainContentAsMarkdownWithTheKey() {
		responseBody = answer(POSTING, 200);

		assertThat(reader.read(URL)).hasValueSatisfying(text -> assertThat(text).contains("Spring Boot"));
		assertThat(lastPath).isEqualTo("/v2/scrape");
		assertThat(lastAuthorization).isEqualTo("Bearer fc-test-key");
		JsonNode request = JSON.readTree(lastBody);
		assertThat(request.path("url").asString()).isEqualTo(URL);
		assertThat(request.path("formats").get(0).asString()).isEqualTo("markdown");
		assertThat(request.path("onlyMainContent").asBoolean()).isTrue();
		assertThat(request.path("timeout").asLong()).isLessThan(5_000);
		assertThat(request.path("maxAge").asLong()).isEqualTo(Duration.ofDays(1).toMillis());
	}

	@Test
	void dropsImagesLinkTargetsAndBareUrls() {
		String text = FirecrawlPageReader.plain(
				"![logo](https://x.test/logo.png) Veja [nossas vagas](https://x.test/careers/python-team) em https://x.test/kotlin hoje.");

		assertThat(text).isEqualTo("Veja nossas vagas em  hoje.");
	}

	@Test
	void givesNothingForTooLittleText() {
		responseBody = answer("Carregando...", 200);

		assertThat(reader.read(URL)).isEmpty();
	}

	@Test
	void givesNothingWhenThePageItselfFailed() {
		responseBody = answer(POSTING, 404);

		assertThat(reader.read(URL)).isEmpty();
	}

	@Test
	void givesNothingOnAnErrorOrAnUnreadableAnswer() {
		status = 402;
		responseBody = "{\"success\":false,\"error\":\"Payment required\"}";
		assertThat(reader.read(URL)).isEmpty();

		status = 200;
		responseBody = "not json";
		assertThat(reader.read(URL)).isEmpty();

		responseBody = "{\"success\":false}";
		assertThat(reader.read(URL)).isEmpty();
	}

	@Test
	void givesNothingWhenFirecrawlTakesTooLong() {
		reader.close();
		reader = reader(Duration.ofMillis(300));
		delayMs = 1_000;
		responseBody = answer(POSTING, 200);

		assertThat(reader.read(URL)).isEmpty();
	}

	@Test
	void truncatesLongPages() {
		responseBody = answer(POSTING + " " + "a".repeat(JobPostingExtractor.MAX_LENGTH), 200);

		assertThat(reader.read(URL))
			.hasValueSatisfying(text -> assertThat(text).hasSize(JobPostingExtractor.MAX_LENGTH));
	}

	@Test
	void neverLogsTheUrlThePageOrTheKey(CapturedOutput log) {
		status = 500;
		responseBody = "{\"success\":false,\"error\":\"" + URL + "\"}";
		reader.read(URL);

		status = 200;
		responseBody = answer(POSTING, 200);
		reader.read(URL);

		assertThat(log.getAll()).doesNotContain("vaga-sigilosa-123")
			.doesNotContain("Spring Boot")
			.doesNotContain("fc-test-key");
	}

	@Test
	void neverShowsTheKey() {
		assertThat(new FirecrawlProperties("fc-secret", URI.create("https://api.firecrawl.dev"), Duration.ofSeconds(1)))
			.hasToString("FirecrawlProperties[apiKey=****, baseUrl=https://api.firecrawl.dev, timeout=PT1S]");
	}

	private FirecrawlPageReader reader(Duration timeout) {
		return new FirecrawlPageReader(new FirecrawlProperties("fc-test-key",
				URI.create("http://127.0.0.1:" + server.getAddress().getPort()), timeout));
	}

	private static String answer(String markdown, int pageStatus) {
		return """
				{"success":true,"data":{"markdown":%s,"metadata":{"title":"Vaga","sourceURL":"%s","statusCode":%d}}}"""
			.formatted(JSON.writeValueAsString(markdown), URL, pageStatus);
	}

}
