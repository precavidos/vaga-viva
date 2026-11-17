package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Optional;

import com.anthropic.client.AnthropicClient;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;

/**
 * Writes the reviewer's comment with Claude. It never throws: any failure,
 * refusal or early stop yields nothing, and the reviewer posts its fixed
 * text (see {@link ClaudeJsonComment}).
 */
final class ClaudeReviewWriter implements ReviewWriter, AutoCloseable {

	private final ClaudeJsonComment claude;

	ClaudeReviewWriter(AnthropicClient client, String model) {
		this.claude = new ClaudeJsonComment(client, model);
	}

	@Override
	public void close() {
		claude.close();
	}

	@Override
	public Optional<String> write(ReviewContext context, Mission focus, List<FailedTest> failing, String contract) {
		return claude.ask(ReviewPrompt.SYSTEM, ReviewPrompt.user(context, focus, failing, contract), "Review text");
	}

}
