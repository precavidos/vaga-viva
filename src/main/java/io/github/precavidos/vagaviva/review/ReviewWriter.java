package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Optional;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;

/**
 * Writes the reviewer's comment about the mission in focus. Empty when no
 * text is available (no LLM configured, a failure, a refusal); the caller
 * then posts the fixed fallback.
 */
public interface ReviewWriter {

	/**
	 * @param focus the first mission of the plan with failing tests
	 * @param failing that mission's failed tests
	 * @param contract the mission's contract from {@code missoes/<id>.md}, or its summary
	 */
	Optional<String> write(ReviewContext context, Mission focus, List<FailedTest> failing, String contract);

}
