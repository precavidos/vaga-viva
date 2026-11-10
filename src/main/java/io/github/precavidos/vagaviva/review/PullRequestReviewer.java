package io.github.precavidos.vagaviva.review;

/** Reviews a pull request after the Vaga Viva workflow finished on it (#24). */
public interface PullRequestReviewer {

	void review(ReviewContext context);

}
