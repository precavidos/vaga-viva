package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Optional;

import com.anthropic.client.AnthropicClient;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;

/**
 * Writes a hint with Claude. It never throws: any failure, refusal or early
 * stop yields nothing, and the hint of that level is the fixed one (see
 * {@link ClaudeJsonComment}).
 */
final class ClaudeHintWriter implements HintWriter, AutoCloseable {

	private final ClaudeJsonComment claude;

	ClaudeHintWriter(AnthropicClient client, String model) {
		this.claude = new ClaudeJsonComment(client, model);
	}

	@Override
	public void close() {
		claude.close();
	}

	@Override
	public Optional<String> write(ReviewContext context, Mission mission, int level, List<FailedTest> failing,
			String contract, List<String> earlierHints) {
		return claude.ask(HintPrompt.SYSTEM, HintPrompt.user(context, mission, level, failing, contract, earlierHints),
				"Hint text");
	}

}
