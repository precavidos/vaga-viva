package io.github.precavidos.vagaviva.review;

/**
 * Answers a student's {@code /dica} comment on a pull request (#27).
 *
 * @param request the comment's text after {@code /dica}, stripped (for example a mission id), possibly empty
 */
public interface HintResponder {

	void respond(ReviewContext context, String request);

}
