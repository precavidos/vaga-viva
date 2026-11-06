package io.github.precavidos.vagaviva.jobposting;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

/**
 * The whole chain with the real fetcher, reader and limiter: only the address
 * policy is loosened so the fetcher may reach a career page served on loopback.
 */
@Import({ TestcontainersConfiguration.class, JobPostingUrlEndToEndTests.LoopbackFetcher.class })
@SpringBootTest
@AutoConfigureMockMvc
class JobPostingUrlEndToEndTests {

	private static HttpServer careerSite;

	@Autowired
	MockMvcTester mvc;

	@BeforeAll
	static void startCareerSite() throws IOException {
		careerSite = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		careerSite.createContext("/vagas/dev-java-junior", exchange -> {
			byte[] body;
			try (InputStream in = JobPostingUrlEndToEndTests.class.getResourceAsStream("/jobposting/pages/json-ld.html")) {
				body = in.readAllBytes();
			}
			exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
			exchange.sendResponseHeaders(200, body.length);
			try (var out = exchange.getResponseBody()) {
				out.write(body);
			}
		});
		careerSite.start();
	}

	@AfterAll
	static void stopCareerSite() {
		careerSite.stop(0);
	}

	@Test
	void extractsAPostingServedOverHttp() {
		String url = "http://127.0.0.1:" + careerSite.getAddress().getPort() + "/vagas/dev-java-junior";

		MvcTestResult response = mvc.post()
			.uri("/api/job-postings/extraction/from-url")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"url\": \"" + url + "\" }")
			.exchange();

		assertThat(response).hasStatusOk();
		assertThat(response).bodyJson().extractingPath("$.seniorityLevels").asArray().containsExactly("JUNIOR");
		assertThat(response).bodyJson()
			.extractingPath("$.technologies[*].id")
			.asArray()
			.contains("java", "spring-boot", "postgresql");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class LoopbackFetcher {

		@Bean
		@Primary
		JobPostingFetcher loopbackFetcher() {
			return new JobPostingFetcher(address -> address.isLoopbackAddress() || AddressPolicy.PUBLIC_ONLY.allows(address),
					false, JobPostingFetcher.TIMEOUT, JobPostingFetcher.MAX_BYTES);
		}

	}

}
