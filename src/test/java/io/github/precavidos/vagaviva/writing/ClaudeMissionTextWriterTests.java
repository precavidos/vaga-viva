package io.github.precavidos.vagaviva.writing;

import static io.github.precavidos.vagaviva.jobposting.SeniorityLevel.JUNIOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.sun.net.httpserver.HttpServer;

import io.github.precavidos.vagaviva.archetype.ArchetypeCatalog;
import io.github.precavidos.vagaviva.archetype.ArchetypeFit;
import io.github.precavidos.vagaviva.jobposting.ExtractedJobPosting;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.plan.PlannedMission;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import io.github.precavidos.vagaviva.plan.ProjectPlanner;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ClaudeMissionTextWriterTests {

	private static final SkillVocabulary VOCABULARY = SkillVocabulary.fromClasspath("vocabulary/java-spring.json");

	private static final ArchetypeCatalog ARCHETYPES = ArchetypeCatalog.fromClasspath("archetypes/catalog.json");

	private static final ProjectPlanner PLANNER = new ProjectPlanner(
			MissionCatalog.fromClasspath("missions/java-spring.json", VOCABULARY));

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private HttpServer server;

	private volatile String lastRequestBody;

	private volatile int status = 200;

	private volatile String responseBody;

	private volatile long delayMillis;

	private ClaudeMissionTextWriter writer;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/v1/messages", exchange -> {
			lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			if (delayMillis > 0) {
				try {
					Thread.sleep(delayMillis);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			try {
				exchange.sendResponseHeaders(status, body.length);
				exchange.getResponseBody().write(body);
			}
			catch (IOException ignored) {
				// the client gave up (timeout test)
			}
			finally {
				exchange.close();
			}
		});
		server.start();
		AnthropicClient client = AnthropicOkHttpClient.builder()
			.apiKey("test-key")
			.baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
			.maxRetries(0)
			.timeout(Duration.ofSeconds(2))
			.build();
		writer = new ClaudeMissionTextWriter(client, "claude-haiku-5-5", VOCABULARY);
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	@Test
	void sendsOnlyThePlanWithThinkingOffAndAStructuredFormat() throws Exception {
		var text = "Vaga júnior Java e Spring Boot. Falar com recrutadora@exemplo.com, Fulana de Tal";
		var posting = new ExtractedJobPosting("java-spring", ARCHETYPES.match(text), List.of(JUNIOR),
				VOCABULARY.recognize(text));
		ProjectPlan plan = PLANNER.plan(posting);
		responseBody = messageWith(answer("Intro.", Map.of()));

		writer.write(plan);

		JsonNode request = JSON.readTree(lastRequestBody);
		assertThat(request.path("model").asString()).isEqualTo("claude-haiku-5-5");
		assertThat(request.path("thinking").path("type").asString()).isEqualTo("disabled");
		assertThat(request.path("output_config").path("effort").asString()).isEqualTo("low");
		assertThat(request.path("output_config").path("format").path("type").asString()).isEqualTo("json_schema");
		assertThat(lastRequestBody).doesNotContain("recrutadora@exemplo.com").doesNotContain("Fulana");
	}

	@Test
	void turnsTheAnswerIntoTexts() {
		ProjectPlan plan = plan();
		var contexts = new LinkedHashMap<String, String>();
		plan.missions().forEach(m -> contexts.put(m.mission().id(), "Contexto de " + m.mission().id()));
		responseBody = messageWith(answer("  Introdução do projeto.  ", contexts));

		WrittenTexts texts = writer.write(plan);

		assertThat(texts.projectIntro()).contains("Introdução do projeto.");
		assertThat(texts.missionContexts()).isEqualTo(Map.copyOf(contexts));
		assertThat(texts.usage()).contains(new TokenUsage(1200, 900));
	}

	@Test
	void ignoresUnknownAndRepeatedMissionIds() {
		ProjectPlan plan = plan();
		String id = plan.missions().get(0).mission().id();
		responseBody = messageWith(rawAnswer("Intro.", List.of(item("nao-existe", "Fora do plano."),
				item(id, "Primeiro."), item(id, "Segundo."))));

		WrittenTexts texts = writer.write(plan);

		assertThat(texts.missionContexts()).containsOnly(Map.entry(id, "Primeiro."));
	}

	@Test
	void dropsTextsThatWouldBreakTheLayout() {
		ProjectPlan plan = plan();
		List<PlannedMission> missions = plan.missions();
		String title = missions.get(0).mission().id();
		String fence = missions.get(1).mission().id();
		String huge = missions.get(2).mission().id();
		String fine = missions.get(3).mission().id();
		responseBody = messageWith(rawAnswer("   ", List.of(item(title, "Texto\n## Título"), item(fence, "Veja ```java```"),
				item(huge, "a".repeat(801)), item(fine, "Bom texto."))));

		WrittenTexts texts = writer.write(plan);

		assertThat(texts.projectIntro()).isEmpty();
		assertThat(texts.missionContexts()).containsOnly(Map.entry(fine, "Bom texto."));
	}

	@Test
	void dropsTextsWithMentionsOrIssueReferences() {
		ProjectPlan plan = plan();
		List<PlannedMission> missions = plan.missions();
		String mention = missions.get(0).mission().id();
		String reference = missions.get(1).mission().id();
		String csharp = missions.get(2).mission().id();
		responseBody = messageWith(rawAnswer("Time @spring-projects e #2.", List.of(
				item(mention, "Como @spring-projects recomenda."), item(reference, "Veja #2 para mais."),
				item(csharp, "C# e Java."))));

		WrittenTexts texts = writer.write(plan);

		assertThat(texts.projectIntro()).isEmpty();
		assertThat(texts.missionContexts()).containsOnly(Map.entry(csharp, "C# e Java."));
	}

	@Test
	void closingTheWriterClosesTheClient() {
		AnthropicClient client = org.mockito.Mockito.mock(AnthropicClient.class);

		new ClaudeMissionTextWriter(client, "claude-haiku-5-5", VOCABULARY).close();

		org.mockito.Mockito.verify(client).close();
	}

	@Test
	void givesNothingOnServerError() {
		status = 500;
		responseBody = "{\"type\":\"error\",\"error\":{\"type\":\"api_error\",\"message\":\"boom\"}}";

		assertThat(writer.write(plan())).isEqualTo(WrittenTexts.none());
	}

	@Test
	void givesNothingOnRefusal() {
		responseBody = message(textBlock(answer("Intro.", Map.of())), "refusal");

		assertThat(writer.write(plan())).isEqualTo(WrittenTexts.none());
	}

	@Test
	void givesNothingOnInvalidJson() {
		responseBody = messageWith("não é json");

		assertThat(writer.write(plan())).isEqualTo(WrittenTexts.none());
	}

	@Test
	void givesNothingWhenTheResponseHasNoText() {
		responseBody = message("", "end_turn");

		assertThat(writer.write(plan())).isEqualTo(WrittenTexts.none());
	}

	@Test
	void givesNothingWhenTheApiIsTooSlow() {
		delayMillis = 3000;
		responseBody = messageWith(answer("Intro.", Map.of()));

		assertThat(writer.write(plan())).isEqualTo(WrittenTexts.none());
	}

	@Test
	void dropsAnIntroThatDoesNotMentionWhatIsNotPracticed() {
		ProjectPlan plan = partialPlan();
		var contexts = new LinkedHashMap<String, String>();
		plan.missions().forEach(m -> contexts.put(m.mission().id(), "Contexto de " + m.mission().id()));
		responseBody = messageWith(answer("Este projeto prepara você para a vaga.", contexts));

		WrittenTexts texts = writer.write(plan);

		assertThat(texts.projectIntro()).isEmpty();
		assertThat(texts.missionContexts()).isEqualTo(Map.copyOf(contexts));
	}

	@Test
	void keepsAnIntroThatMentionsWhatIsNotPracticed() {
		responseBody = messageWith(answer("A vaga pede QUARKUS, que o projeto não pratica.", Map.of()));

		WrittenTexts texts = writer.write(partialPlan());

		assertThat(texts.projectIntro()).contains("A vaga pede QUARKUS, que o projeto não pratica.");
	}

	@Test
	void aNameInsideAnotherWordDoesNotCount() {
		var fit = new ArchetypeFit("java-spring", ArchetypeFit.Status.PARTIAL, List.of("Java"), List.of("Go"));
		responseBody = messageWith(answer("Você vai praticar algo novo com Django.", Map.of()));

		WrittenTexts texts = writer.write(planWith(fit));

		assertThat(texts.projectIntro()).isEmpty();
	}

	@Test
	void aPrefixOfALongerNameDoesNotCount() {
		var fit = new ArchetypeFit("java-spring", ArchetypeFit.Status.NOT_COVERED, List.of(), List.of("Java"));
		responseBody = messageWith(answer("Você vai praticar JavaScript no projeto.", Map.of()));

		WrittenTexts texts = writer.write(planWith(fit));

		assertThat(texts.projectIntro()).isEmpty();
	}

	@Test
	void namesWithPunctuationStillMatch() {
		var fit = new ArchetypeFit("java-spring", ArchetypeFit.Status.PARTIAL, List.of("Java"),
				List.of("Node.js", "C#"));
		responseBody = messageWith(answer("Este projeto não pratica Node.js nem C#, mas ensina o básico.", Map.of()));

		WrittenTexts texts = writer.write(planWith(fit));

		assertThat(texts.projectIntro()).contains("Este projeto não pratica Node.js nem C#, mas ensina o básico.");
	}

	private static ProjectPlan planWith(ArchetypeFit fit) {
		return PLANNER.plan(List.of(JUNIOR), List.of("java"), List.of("java"), fit);
	}

	private static ProjectPlan partialPlan() {
		return planWith(new ArchetypeFit("java-spring", ArchetypeFit.Status.PARTIAL, List.of("Java"), List.of("Quarkus")));
	}

	private static ProjectPlan plan() {
		var text = "Vaga júnior Java e Spring Boot com SQL";
		var posting = new ExtractedJobPosting("java-spring", ARCHETYPES.match(text), List.of(JUNIOR),
				VOCABULARY.recognize(text));
		return PLANNER.plan(posting);
	}

	private static String answer(String intro, Map<String, String> contexts) {
		return rawAnswer(intro, contexts.entrySet().stream().map(e -> item(e.getKey(), e.getValue())).toList());
	}

	private static Map<String, String> item(String id, String context) {
		return Map.of("id", id, "context", context);
	}

	private static String rawAnswer(String intro, List<Map<String, String>> missions) {
		return JSON.writeValueAsString(Map.of("projectIntro", intro, "missions", missions));
	}

	private static String textBlock(String text) {
		return JSON.writeValueAsString(Map.of("type", "text", "text", text));
	}

	private static String messageWith(String text) {
		return message(textBlock(text), "end_turn");
	}

	private static String message(String contentBlocks, String stopReason) {
		return """
				{"id":"msg_test","type":"message","role":"assistant","model":"claude-haiku-5-5",
				"content":[%s],"stop_reason":"%s","stop_sequence":null,
				"usage":{"input_tokens":1200,"output_tokens":900}}"""
			.formatted(contentBlocks, stopReason);
	}

}
