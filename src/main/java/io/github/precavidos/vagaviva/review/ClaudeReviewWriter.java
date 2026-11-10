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

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes the reviewer's comment with Claude, asking for structured JSON. It
 * never throws: any failure, refusal or stop other than {@code end_turn}
 * yields nothing, and the reviewer posts its fixed text. Only the model and
 * token counts are logged, never the diff or the answer.
 */
final class ClaudeReviewWriter implements ReviewWriter, AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(ClaudeReviewWriter.class);

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final long MAX_TOKENS = 2_000L;

	private final AnthropicClient client;

	private final String model;

	ClaudeReviewWriter(AnthropicClient client, String model) {
		this.client = client;
		this.model = model;
	}

	@Override
	public void close() {
		client.close();
	}

	@Override
	public Optional<String> write(ReviewContext context, Mission focus, List<FailedTest> failing, String contract) {
		try {
			Message message = client.messages().create(request(context, focus, failing, contract));
			if (!message.stopReason().map(StopReason.END_TURN::equals).orElse(false)) {
				log.warn("Review text unavailable: {}", message.stopReason().map(Object::toString).orElse("no stop_reason"));
				return Optional.empty();
			}
			String json = message.content()
				.stream()
				.flatMap(block -> block.text().stream())
				.findFirst()
				.map(block -> block.text())
				.orElseThrow(() -> new IllegalStateException("no text block"));
			String comment = JSON.readValue(json, Answer.class).comment();
			log.info("Review text written: model={}, inputTokens={}, outputTokens={}", model,
					message.usage().inputTokens(), message.usage().outputTokens());
			return Optional.ofNullable(comment).map(String::strip).filter(text -> !text.isEmpty());
		}
		catch (RuntimeException e) {
			log.warn("Review text unavailable: {}", e.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	private MessageCreateParams request(ReviewContext context, Mission focus, List<FailedTest> failing,
			String contract) {
		return MessageCreateParams.builder()
			.model(model)
			.maxTokens(MAX_TOKENS)
			.system(ReviewPrompt.SYSTEM)
			.addUserMessage(ReviewPrompt.user(context, focus, failing, contract))
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
