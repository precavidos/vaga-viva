package io.github.precavidos.vagaviva.jobposting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JobPostingFetcherTests {

	private static final AddressPolicy LOOPBACK_ALLOWED = address -> address.isLoopbackAddress()
			|| AddressPolicy.PUBLIC_ONLY.allows(address);

	private final Map<String, Route> routes = new ConcurrentHashMap<>();

	private final AtomicInteger requests = new AtomicInteger();

	private final AtomicReference<String> userAgent = new AtomicReference<>();

	private HttpServer server;

	private JobPostingFetcher production;

	private JobPostingFetcher local;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.setExecutor(Executors.newCachedThreadPool());
		server.createContext("/", exchange -> {
			requests.incrementAndGet();
			userAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
			Route route = routes.get(exchange.getRequestURI().getPath());
			if (route == null) {
				respond(exchange, 404, null, new byte[0]);
			}
			else {
				route.handle(exchange);
			}
		});
		server.start();
		production = new JobPostingFetcher();
		local = new JobPostingFetcher(LOOPBACK_ALLOWED, false, Duration.ofMillis(500), 1_000);
	}

	@AfterEach
	void stopServer() throws Exception {
		server.stop(0);
		production.close();
		local.close();
	}

	@Test
	void rejectsLoopback() {
		routes.put("/", html("<p>interno</p>"));

		assertThatThrownBy(() -> production.fetch(url("/"))).isInstanceOf(UnsafeUrlException.class);
		assertThat(requests).hasValue(0);
	}

	@Test
	void rejectsLocalhostByName() {
		routes.put("/", html("<p>interno</p>"));

		assertThatThrownBy(() -> production.fetch("http://localhost:" + port() + "/"))
			.isInstanceOf(UnsafeUrlException.class);
		assertThat(requests).hasValue(0);
	}

	@Test
	void rejectsCloudMetadataAddress() {
		assertThatThrownBy(() -> production.fetch("http://169.254.169.254/latest/meta-data/"))
			.isInstanceOf(UnsafeUrlException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "file:///etc/passwd", "ftp://example.com/vaga", "jar:file:/x!/y" })
	void rejectsNonHttpSchemes(String url) {
		assertThatThrownBy(() -> production.fetch(url)).isInstanceOf(UnsafeUrlException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = { "não é url", "http://", "https:///vaga", "" })
	void rejectsMalformedUrls(String url) {
		assertThatThrownBy(() -> production.fetch(url)).isInstanceOf(UnsafeUrlException.class);
	}

	@Test
	void rejectsRedirectToPrivateAddress() {
		routes.put("/start", redirect("http://10.0.0.1/"));

		assertThatThrownBy(() -> local.fetch(url("/start"))).isInstanceOf(UnsafeUrlException.class);
	}

	@Test
	void rejectsRedirectToFileScheme() {
		routes.put("/start", redirect("file:///etc/passwd"));

		assertThatThrownBy(() -> local.fetch(url("/start"))).isInstanceOf(UnsafeUrlException.class);
	}

	@Test
	void downloadsHtmlWithTheHeaderCharset() {
		byte[] body = "<p>Vaga Júnior</p>".getBytes(StandardCharsets.ISO_8859_1);
		routes.put("/vaga", exchange -> respond(exchange, 200, "text/html; charset=ISO-8859-1", body));

		FetchedPage page = local.fetch(url("/vaga"));

		assertThat(page.charset()).isEqualTo("ISO-8859-1");
		assertThat(page.html()).isEqualTo(body);
		assertThat(userAgent).hasValue("VagaViva/0.1");
	}

	@Test
	void charsetIsNullWhenTheHeaderHasNone() {
		routes.put("/vaga", html("<p>Vaga</p>"));

		assertThat(local.fetch(url("/vaga")).charset()).isNull();
	}

	@Test
	void acceptsXhtml() {
		routes.put("/vaga", exchange -> respond(exchange, 200, "application/xhtml+xml", bytes("<p>Vaga</p>")));

		assertThat(local.fetch(url("/vaga")).html()).isEqualTo(bytes("<p>Vaga</p>"));
	}

	@Test
	void followsARelativeRedirect() {
		routes.put("/a", redirect("/b"));
		routes.put("/b", html("<p>destino</p>"));

		assertThat(local.fetch(url("/a")).html()).isEqualTo(bytes("<p>destino</p>"));
	}

	@Test
	void followsUpToThreeRedirects() {
		routes.put("/r0", redirect("/r1"));
		routes.put("/r1", redirect("/r2"));
		routes.put("/r2", redirect("/r3"));
		routes.put("/r3", redirect("/ok"));
		routes.put("/ok", html("<p>ok</p>"));

		assertThat(local.fetch(url("/r1")).html()).isEqualTo(bytes("<p>ok</p>"));
		assertThatThrownBy(() -> local.fetch(url("/r0"))).isInstanceOf(PageFetchException.class);
	}

	@Test
	void errorStatusFails() {
		assertThatThrownBy(() -> local.fetch(url("/nao-existe"))).isInstanceOf(PageFetchException.class);
	}

	@Test
	void redirectWithoutLocationFails() {
		routes.put("/start", exchange -> respond(exchange, 302, null, new byte[0]));

		assertThatThrownBy(() -> local.fetch(url("/start"))).isInstanceOf(PageFetchException.class);
	}

	@Test
	void tooLargeBodyFails() {
		routes.put("/grande", exchange -> respond(exchange, 200, "text/html", new byte[1_001]));
		routes.put("/limite", exchange -> respond(exchange, 200, "text/html", new byte[1_000]));

		assertThatThrownBy(() -> local.fetch(url("/grande"))).isInstanceOf(PageFetchException.class);
		assertThat(local.fetch(url("/limite")).html()).hasSize(1_000);
	}

	@Test
	void slowServerTimesOut() {
		routes.put("/lento", exchange -> {
			try {
				Thread.sleep(2_000);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			respond(exchange, 200, "text/html", bytes("<p>tarde</p>"));
		});

		long start = System.nanoTime();
		assertThatThrownBy(() -> local.fetch(url("/lento"))).isInstanceOf(PageFetchException.class);
		assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
	}

	@Test
	void abortsAServerThatDripsBytesPastTheOverallDeadline() {
		routes.put("/gotejando", exchange -> {
			exchange.getResponseHeaders().set("Content-Type", "text/html");
			exchange.sendResponseHeaders(200, 0);
			try (var out = exchange.getResponseBody()) {
				for (int i = 0; i < 50; i++) {
					out.write('a');
					out.flush();
					Thread.sleep(100);
				}
			}
			catch (InterruptedException | IOException ex) {
				// the client hung up, as expected
			}
		});

		long start = System.nanoTime();
		assertThatThrownBy(() -> local.fetch(url("/gotejando"))).isInstanceOf(PageFetchException.class);
		assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(1_500));
	}

	@ParameterizedTest
	@ValueSource(strings = { "http://example.com:8080/vaga", "https://example.com:8443/vaga", "http://example.com:25/",
			"http://example.com:6379/" })
	void rejectsNonDefaultPortsInProduction(String url) {
		assertThatThrownBy(() -> production.fetch(url)).isInstanceOf(UnsafeUrlException.class);
	}

	@Test
	void rejectsOutOfRangePorts() {
		routes.put("/start", redirect("http://127.0.0.1:99999/"));

		assertThatThrownBy(() -> local.fetch("http://127.0.0.1:99999/")).isInstanceOf(UnsafeUrlException.class);
		assertThatThrownBy(() -> local.fetch(url("/start"))).isInstanceOf(UnsafeUrlException.class);
	}

	@Test
	void ignoresWhitespaceAroundTheUrl() {
		routes.put("/vaga", html("<p>Vaga</p>"));

		assertThat(local.fetch("  " + url("/vaga") + "\n").html()).isEqualTo(bytes("<p>Vaga</p>"));
	}

	@Test
	void nonHtmlIsUnreadable() {
		routes.put("/vaga.pdf", exchange -> respond(exchange, 200, "application/pdf", bytes("%PDF-1.7")));

		assertThatThrownBy(() -> local.fetch(url("/vaga.pdf"))).isInstanceOf(UnreadablePageException.class);
	}

	@Test
	void exceptionsDoNotCarryTheUrl() {
		routes.put("/vaga-sigilosa", redirect("http://10.0.0.1/"));

		assertThatThrownBy(() -> local.fetch(url("/vaga-sigilosa"))).satisfies(e -> {
			assertThat(e.getMessage()).doesNotContain("10.0.0.1").doesNotContain("vaga-sigilosa");
			assertThat(e.getCause()).isNull();
		});
		assertThatThrownBy(() -> local.fetch(url("/vaga-sigilosa-404"))).satisfies(e -> {
			assertThat(e.getMessage()).doesNotContain("vaga-sigilosa").doesNotContain(String.valueOf(port()));
			assertThat(e.getCause()).isNull();
		});
	}

	private String url(String path) {
		return "http://127.0.0.1:" + port() + path;
	}

	private int port() {
		return server.getAddress().getPort();
	}

	private static byte[] bytes(String text) {
		return text.getBytes(StandardCharsets.UTF_8);
	}

	private static Route html(String body) {
		return exchange -> respond(exchange, 200, "text/html", bytes(body));
	}

	private static Route redirect(String location) {
		return exchange -> {
			exchange.getResponseHeaders().set("Location", location);
			respond(exchange, 302, null, new byte[0]);
		};
	}

	private static void respond(HttpExchange exchange, int status, String contentType, byte[] body)
			throws IOException {
		if (contentType != null) {
			exchange.getResponseHeaders().set("Content-Type", contentType);
		}
		exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
		try (var out = exchange.getResponseBody()) {
			out.write(body);
		}
	}

	@FunctionalInterface
	private interface Route {

		void handle(HttpExchange exchange) throws IOException;

	}

}
