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
 * One Claude call that answers with a single comment, as structured JSON
 * {@code {"comment": "..."}}: the reviewer's and the hints' shared call. It
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

	/** @param purpose names the text in the logs, such as {@code "Review text"} */
	Optional<String> ask(String system, String user, String purpose) {
		try {
			Message message = client.messages().create(request(system, user));
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
			String comment = JSON.readValue(json, Answer.class).comment();
			log.info("{} written: model={}, inputTokens={}, outputTokens={}", purpose, model,
					message.usage().inputTokens(), message.usage().outputTokens());
			return Optional.ofNullable(comment).map(String::strip).filter(text -> !text.isEmpty());
		}
		catch (RuntimeException e) {
			log.warn("{} unavailable: {}", purpose, e.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	private MessageCreateParams request(String system, String user) {
		return MessageCreateParams.builder()
			.model(model)
			.maxTokens(MAX_TOKENS)
			.system(system)
			.addUserMessage(user)
			.thinking(ThinkingConfigDisabled.builder().build())
			.outputConfig(OutputConfig.builder()
				.effort(OutputConfig.Effort.MEDIUM)
				.format(JsonOutputFormat.builder().schema(schema()).build())
				.build())
			.build();
	}

	private static JsonOutputFormat.Schema schema() {
		Map<String, Object> schema = Map.of("type", "object", "additionalProperties", false, "required",
				List.of("comment"), "properties", Map.of("comment", Map.of("type", "string")));
		var builder = JsonOutputFormat.Schema.builder();
		schema.forEach((key, value) -> builder.putAdditionalProperty(key, JsonValue.from(value)));
		return builder.build();
	}

	private record Answer(String comment) {
	}

}
