package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Optional;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;

/**
 * Writes the two parts of the reviewer's comment that are prose: what already works and the questions. Empty when
 * no text is available (no LLM configured, a failure, a refusal); the reviewer then uses its fixed texts. Each part
 * is checked by {@link ReviewTextFilter} on its own, so one refused sentence does not drop the rest.
 */
public interface ReviewWriter {

	/**
	 * @param focus the first mission of the plan with failing tests
	 * @param failing that mission's failed tests
	 * @param contract the mission's contract from {@code missoes/<id>.md}, or its summary
	 */
	Optional<Written> write(ReviewContext context, Mission focus, List<FailedTest> failing, String contract);

	/**
	 * @param working one or two sentences on what the student already got right
	 * @param questions two or three questions, one each, without numbering
	 */
	record Written(String working, List<String> questions) {

		public Written {
			working = working == null ? "" : working.strip();
			questions = questions == null ? List.of() : List.copyOf(questions);
		}

	}

}
