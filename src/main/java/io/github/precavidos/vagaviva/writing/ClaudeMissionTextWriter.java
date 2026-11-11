package io.github.precavidos.vagaviva.writing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.ThinkingConfigDisabled;

import io.github.precavidos.vagaviva.archetype.ArchetypeFit;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import io.github.precavidos.vagaviva.vocabulary.Vocabularies;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes the student texts with Claude Haiku through the Anthropic API, asking
 * for structured JSON. It never throws: any failure yields
 * {@link WrittenTexts#none()}, so every text falls back to its default.
 */
public final class ClaudeMissionTextWriter implements MissionTextWriter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ClaudeMissionTextWriter.class);

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final long MAX_TOKENS = 4000L;

	private static final int MAX_INTRO_LENGTH = 1500;

	private static final int MAX_CONTEXT_LENGTH = 800;

	/** A GitHub mention such as {@code @user}; an e-mail address is not one. */
	private static final Pattern MENTION = Pattern.compile("(?<![\\w.])@[A-Za-z0-9]");

	/** An issue reference such as {@code #2}; "C#" is not one. */
	private static final Pattern ISSUE_REFERENCE = Pattern.compile("(?<![\\w&])#\\d");

	private final AnthropicClient client;

	private final String model;

	private final Vocabularies vocabularies;

	public ClaudeMissionTextWriter(AnthropicClient client, String model, Vocabularies vocabularies) {
		this.client = client;
		this.model = model;
		this.vocabularies = vocabularies;
	}

	@Override
	public void close() {
		client.close();
	}

	@Override
	public WrittenTexts write(ProjectPlan plan) {
		long start = System.nanoTime();
		try {
			Message message = client.messages().create(request(plan));
			Optional<StopReason> stopReason = message.stopReason();
			if (stopReason.isEmpty() || !stopReason.get().equals(StopReason.END_TURN)) {
				LOGGER.warn("LLM texts unavailable: {}", stopReason.map(Object::toString).orElse("no stop_reason"));
				return WrittenTexts.none();
			}
			String json = message.content()
				.stream()
				.flatMap(block -> block.text().stream())
				.findFirst()
				.map(block -> block.text())
				.orElseThrow(() -> new IllegalStateException("no text block"));
			Answer answer = JSON.readValue(json, Answer.class);
			var usage = new TokenUsage(message.usage().inputTokens(), message.usage().outputTokens());
			WrittenTexts texts = new WrittenTexts(intro(plan, answer), contexts(plan, answer), Optional.of(usage));
			LOGGER.info("LLM texts written: model={}, inputTokens={}, outputTokens={}, millis={}", model,
					usage.inputTokens(), usage.outputTokens(), (System.nanoTime() - start) / 1_000_000);
			return texts;
		}
		catch (RuntimeException e) {
			LOGGER.warn("LLM texts unavailable: {}", e.getClass().getSimpleName());
			return WrittenTexts.none();
		}
	}

	private MessageCreateParams request(ProjectPlan plan) {
		return MessageCreateParams.builder()
			.model(model)
			.maxTokens(MAX_TOKENS)
			.system(MissionTextPrompt.SYSTEM)
			.addUserMessage(MissionTextPrompt.user(plan, vocabularies.get(plan.archetype())))
			.thinking(ThinkingConfigDisabled.builder().build())
			.outputConfig(OutputConfig.builder()
				.effort(OutputConfig.Effort.LOW)
				.format(JsonOutputFormat.builder().schema(schema()).build())
				.build())
			.build();
	}

	private static JsonOutputFormat.Schema schema() {
		var string = Map.of("type", "string");
		var item = Map.of("type", "object", "additionalProperties", false, "required", List.of("id", "context"),
				"properties", Map.of("id", string, "context", string));
		var properties = Map.of("projectIntro", string, "missions", Map.of("type", "array", "items", item));
		Map<String, Object> schema = Map.of("type", "object", "additionalProperties", false, "required",
				List.of("projectIntro", "missions"), "properties", properties);
		var values = new HashMap<String, JsonValue>();
		schema.forEach((key, value) -> values.put(key, JsonValue.from(value)));
		return JsonOutputFormat.Schema.builder().putAllAdditionalProperties(values).build();
	}

	/**
	 * When the project does not practice part of the posting's stack, the
	 * intro is usable only if it names every such technology; otherwise the
	 * default intro, which always carries the warning, is used.
	 */
	private static Optional<String> intro(ProjectPlan plan, Answer answer) {
		return Optional.ofNullable(answer.projectIntro())
			.map(String::strip)
			.filter(t -> isUsable(t, MAX_INTRO_LENGTH))
			.filter(t -> mentionsWhatIsNotPracticed(plan.archetypeFit(), t));
	}

	private static boolean mentionsWhatIsNotPracticed(ArchetypeFit fit, String intro) {
		if (fit.status() != ArchetypeFit.Status.PARTIAL && fit.status() != ArchetypeFit.Status.NOT_COVERED) {
			return true;
		}
		return fit.uncovered().stream().allMatch(name -> namedIn(name, intro));
	}

	/** Whether the name appears as a whole word: not glued to a letter or digit on either side. */
	private static boolean namedIn(String name, String intro) {
		return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(name) + "(?![\\p{L}\\p{N}])",
				Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
			.matcher(intro)
			.find();
	}

	private static Map<String, String> contexts(ProjectPlan plan, Answer answer) {
		Set<String> planned = new HashSet<>();
		plan.missions().forEach(m -> planned.add(m.mission().id()));
		var contexts = new HashMap<String, String>();
		Set<String> seen = new HashSet<>();
		if (answer.missions() == null) {
			return contexts;
		}
		for (Item item : answer.missions()) {
			if (item == null || item.id() == null || !planned.contains(item.id()) || !seen.add(item.id())) {
				continue;
			}
			String context = item.context() == null ? "" : item.context().strip();
			if (isUsable(context, MAX_CONTEXT_LENGTH)) {
				contexts.put(item.id(), context);
			}
		}
		return contexts;
	}

	private static boolean isUsable(String text, int maxLength) {
		return !text.isEmpty() && text.length() <= maxLength && !text.contains("```")
				&& text.lines().noneMatch(line -> line.stripLeading().startsWith("#"))
				&& !MENTION.matcher(text).find() && !ISSUE_REFERENCE.matcher(text).find();
	}

	private record Answer(String projectIntro, List<Item> missions) {
	}

	private record Item(String id, String context) {
	}

}
