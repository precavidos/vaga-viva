package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Optional;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;

/**
 * Writes one hint about a mission. Empty when no text is available (no LLM
 * configured, a failure, a refusal); the caller then posts the fixed hint of
 * that level.
 */
public interface HintWriter {

	/**
	 * @param level 1 (direction), 2 (approach) or 3 (almost there)
	 * @param failing the mission's failed tests, possibly empty when the report is not available
	 * @param contract the mission's contract from {@code missoes/<id>.md}, or its summary
	 * @param earlierHints the texts of the earlier hints about this mission, oldest first
	 */
	Optional<String> write(ReviewContext context, Mission mission, int level, List<FailedTest> failing,
			String contract, List<String> earlierHints);

}
