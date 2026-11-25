package io.github.precavidos.vagaviva.review;

import java.util.Optional;

import io.github.precavidos.vagaviva.review.ReviewContext.PreviousComment;

/**
 * Recognizes the reviewer's own comment and reads it back as plain text. The comment itself is built by
 * {@link ReviewComment}.
 */
public final class ReviewTexts {

	/**
	 * The first line of a review comment written before the summary existed. Newer comments open with
	 * {@link ReviewComment#MARKER_PREFIX} followed by the mission in focus; both are reviews.
	 */
	static final String MARKER = "<!-- vaga-viva:review -->";

	/** The footer of review comments written before {@link ReviewComment#FOOTER}. */
	static final String FOOTER = "\n\n---\n<sub>Revisor do Vaga Viva. Travou? Comente `/dica` neste PR para receber uma dica, uma de cada vez.</sub>";

	private ReviewTexts() {
	}

	/** Whether {@code comment} is the reviewer's: its marker opens the body. */
	static boolean isReview(PreviousComment comment) {
		return comment.body().startsWith(ReviewComment.MARKER_PREFIX);
	}

	/**
	 * What the reviewer's own comment says, as plain text: what already works and the questions, without the
	 * tables and titles; empty for any other comment.
	 */
	public static Optional<String> reviewText(String body) {
		return ReviewComment.plainText(body);
	}

}
