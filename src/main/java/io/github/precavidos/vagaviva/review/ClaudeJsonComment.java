package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

import tools.jackson.databind.json.JsonMapper;

/**
 * One Claude call that answers with structured JSON: a single comment
 * {@code {"comment": "..."}} for the hints, or the reviewer's fields
 * {@code {"working": "...", "questions": [...]}}. It
 * never throws: any failure, refusal or stop other than {@code end_turn}
 * yields nothing, and the caller posts its fixed text. Only the model and
 * token counts are logged, never the prompt or the answer.
 */
final class ClaudeJsonComment implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(ClaudeJsonComment.class);

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final long MAX_TOKENS = 2_000L;

	private final AnthropicClient client;

	private final String model;

	ClaudeJsonComment(AnthropicClient client, String model) {
		this.client = client;
		this.model = model;
	}

	@Override
	public void close() {
		client.close();
	}

	/** @param purpose names the text in the logs, such as {@code "Hint text"} */
	Optional<String> ask(String system, String user, String purpose) {
		return call(system, user, purpose, COMMENT_SCHEMA, Answer.class).map(Answer::comment)
			.map(String::strip)
			.filter(text -> !text.isEmpty());
	}

	/**
	 * The reviewer's fields: what already works and the questions, each checked by the caller on its own.
	 * @param purpose names the text in the logs, such as {@code "Review text"}
	 */
	Optional<ReviewAnswer> askReview(String system, String user, String purpose) {
		return call(system, user, purpose, REVIEW_SCHEMA, ReviewAnswer.class);
	}

	private <T> Optional<T> call(String system, String user, String purpose, Map<String, Object> schema,
			Class<T> type) {
		try {
			Message message = client.messages().create(request(system, user, schema));
			if (!message.stopReason().map(StopReason.END_TURN::equals).orElse(false)) {
				log.warn("{} unavailable: {}", purpose,
						message.stopReason().map(Object::toString).orElse("no stop_reason"));
				return Optional.empty();
			}
			String json = message.content()
				.stream()
				.flatMap(block -> block.text().stream())
				.findFirst()
				.map(block -> block.text())
				.orElseThrow(() -> new IllegalStateException("no text block"));
			T answer = JSON.readValue(json, type);
			log.info("{} written: model={}, inputTokens={}, outputTokens={}", purpose, model,
					message.usage().inputTokens(), message.usage().outputTokens());
			return Optional.ofNullable(answer);
		}
		catch (RuntimeException e) {
			log.warn("{} unavailable: {}", purpose, e.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	private MessageCreateParams request(String system, String user, Map<String, Object> schema) {
		return MessageCreateParams.builder()
			.model(model)
			.maxTokens(MAX_TOKENS)
			.system(system)
			.addUserMessage(user)
			.thinking(ThinkingConfigDisabled.builder().build())
			.outputConfig(OutputConfig.builder()
				.effort(OutputConfig.Effort.MEDIUM)
				.format(JsonOutputFormat.builder().schema(schema(schema)).build())
				.build())
			.build();
	}

	private static final Map<String, Object> COMMENT_SCHEMA = Map.of("type", "object", "additionalProperties", false,
			"required", List.of("comment"), "properties", Map.of("comment", Map.of("type", "string")));

	private static final Map<String, Object> REVIEW_SCHEMA = Map.of("type", "object", "additionalProperties", false,
			"required", List.of("working", "questions"), "properties",
			Map.of("working", Map.of("type", "string"), "questions",
					Map.of("type", "array", "items", Map.of("type", "string"))));

	private static JsonOutputFormat.Schema schema(Map<String, Object> schema) {
		var builder = JsonOutputFormat.Schema.builder();
		schema.forEach((key, value) -> builder.putAdditionalProperty(key, JsonValue.from(value)));
		return builder.build();
	}

	private record Answer(String comment) {
	}

	/**
	 * The reviewer's answer before the filter: {@code working} says in one or two sentences what the student got
	 * right, {@code questions} are two or three questions, one each.
	 */
	record ReviewAnswer(String working, List<String> questions) {

		ReviewAnswer {
			working = working == null ? "" : working.strip();
			questions = questions == null ? List.of()
					: questions.stream().filter(q -> q != null && !q.isBlank()).map(String::strip).toList();
		}

	}

}
