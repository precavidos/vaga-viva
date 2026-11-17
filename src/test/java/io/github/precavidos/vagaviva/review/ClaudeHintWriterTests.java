package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.sun.net.httpserver.HttpServer;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The hints' Claude call, against a local fake of the Messages API. */
class ClaudeHintWriterTests {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final Mission PERSISTENCE = MissionCatalog
		.fromClasspath("missions/java-spring.json", SkillVocabulary.fromClasspath("vocabulary/java-spring.json"))
		.missions()
		.stream()
		.filter(m -> m.id().equals("persistence"))
		.findFirst()
		.orElseThrow();

	private HttpServer server;

	private volatile String lastRequestBody;

	private volatile int status = 200;

	private volatile String responseBody;

	private ClaudeHintWriter writer;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/v1/messages", exchange -> {
			lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		writer = new ClaudeHintWriter(AnthropicOkHttpClient.builder()
			.apiKey("test-key")
			.baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
			.maxRetries(0)
			.timeout(Duration.ofSeconds(2))
			.build(), "claude-haiku-5-5");
	}

	@AfterEach
	void stopServer() {
		writer.close();
		server.stop(0);
	}

	@Test
	void sendsTheLevelAndReturnsTheComment() throws Exception {
		responseBody = message("{\"comment\": \"O que acontece com a data?\"}", "end_turn");

		assertThat(writer.write(context(), PERSISTENCE, 1, List.of(), "contrato", List.of()))
			.contains("O que acontece com a data?");
		JsonNode request = JSON.readTree(lastRequestBody);
		assertThat(request.path("model").asString()).isEqualTo("claude-haiku-5-5");
		assertThat(request.path("system").toString()).contains("Nível 1");
		assertThat(request.path("messages").get(0).toString()).contains("Nível da dica: 1 de 3")
			.doesNotContain("secret-token");
	}

	@Test
	void givesNothingWhenClaudeStopsEarlyOrFails() {
		responseBody = message("{\"comment\": \"parcial\"}", "max_tokens");
		assertThat(writer.write(context(), PERSISTENCE, 1, List.of(), "c", List.of())).isEmpty();

		status = 500;
		responseBody = "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"boom\"}}";
		assertThat(writer.write(context(), PERSISTENCE, 1, List.of(), "c", List.of())).isEmpty();
	}

	private static ReviewContext context() {
		return new ReviewContext("secret-token", "maria/repo", 7, "abc",
				new StudentPlan("java-spring", "clinic-scheduling", List.of(PERSISTENCE)), "diff --git a/X b/X", true,
				List.of(), List.of());
	}

	private static String message(String text, String stopReason) {
		String content = "{\"type\":\"text\",\"text\":" + JSON.writeValueAsString(text) + "}";
		return """
				{"id":"msg_test","type":"message","role":"assistant","model":"claude-haiku-5-5",
				"content":[%s],"stop_reason":"%s","stop_sequence":null,
				"usage":{"input_tokens":3000,"output_tokens":200}}""".formatted(content, stopReason);
	}

}
