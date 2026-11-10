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
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.PreviousComment;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The reviewer's Claude call, against a local fake of the Messages API. */
class ClaudeReviewWriterTests {

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

	private ClaudeReviewWriter writer;

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
		writer = new ClaudeReviewWriter(AnthropicOkHttpClient.builder()
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
	void sendsTheMissionTheFailingTestsAndTheDiffButNeverTheToken() throws Exception {
		responseBody = message("{\"comment\": \"O que o teste espera?\"}", "end_turn");

		var text = writer.write(context(), PERSISTENCE, List.of(failed()), "# Contrato");

		assertThat(text).contains("O que o teste espera?");
		JsonNode request = JSON.readTree(lastRequestBody);
		assertThat(request.path("model").asString()).isEqualTo("claude-haiku-5-5");
		assertThat(request.path("thinking").path("type").asString()).isEqualTo("disabled");
		assertThat(request.path("output_config").path("format").path("type").asString()).isEqualTo("json_schema");
		assertThat(request.path("system").toString()).contains("Nunca escreva código");
		String user = request.path("messages").get(0).toString();
		assertThat(user).contains("Pacientes no banco de dados")
			.contains("gravaNoBanco: esperava a linha gravada na tabela patients")
			.contains("# Contrato")
			.contains("diff --git a/src/Patient.java")
			.contains("Revisão anterior")
			.doesNotContain("ghs_installation_token");
	}

	@Test
	void givesNothingWhenClaudeRefusesOrStopsEarly() {
		responseBody = message("{\"comment\": \"parcial\"}", "max_tokens");
		assertThat(writer.write(context(), PERSISTENCE, List.of(failed()), "c")).isEmpty();

		responseBody = message("", "refusal");
		assertThat(writer.write(context(), PERSISTENCE, List.of(failed()), "c")).isEmpty();
	}

	@Test
	void givesNothingOnAnErrorOrAnUnreadableAnswer() {
		status = 500;
		responseBody = "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"boom\"}}";
		assertThat(writer.write(context(), PERSISTENCE, List.of(failed()), "c")).isEmpty();

		status = 200;
		responseBody = message("not json", "end_turn");
		assertThat(writer.write(context(), PERSISTENCE, List.of(failed()), "c")).isEmpty();
	}

	private static ReviewContext context() {
		var plan = new StudentPlan("java-spring", "clinic-scheduling", List.of(PERSISTENCE));
		return new ReviewContext("ghs_installation_token", "maria/repo", 7, "abc", plan,
				"diff --git a/src/Patient.java b/src/Patient.java", true, List.of(failed()),
				List.of(new PreviousComment(1, ReviewTexts.MARKER + "\nRevisão anterior" + ReviewTexts.FOOTER)));
	}

	private static FailedTest failed() {
		return new FailedTest("persistence", "gravaNoBanco", "esperava a linha gravada na tabela patients");
	}

	private static String message(String text, String stopReason) {
		String content = text.isEmpty() ? "" : "{\"type\":\"text\",\"text\":" + JSON.writeValueAsString(text) + "}";
		return """
				{"id":"msg_test","type":"message","role":"assistant","model":"claude-haiku-5-5",
				"content":[%s],"stop_reason":"%s","stop_sequence":null,
				"usage":{"input_tokens":3000,"output_tokens":200}}""".formatted(content, stopReason);
	}

}
