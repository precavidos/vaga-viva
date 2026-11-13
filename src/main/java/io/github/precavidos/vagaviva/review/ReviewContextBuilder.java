package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubComment;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.review.ReviewContext.PreviousComment;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;

/**
 * Reads, with an installation token, what the reviewer needs about a pull
 * request. A repository without {@code .vaga-viva/plan.json}, or with a plan
 * for an archetype Vaga Viva does not know, is not a student repository:
 * nothing is built for it.
 */
@Component
class ReviewContextBuilder {

	/** Enough for a mission's change; a larger diff is cut, with a marker. */
	static final int MAX_DIFF_LENGTH = 60_000;

	private final GitHubApi github;

	private final StudentPlans plans;

	private final FailedTestsSource failedTests;

	private final GitHubAppProperties app;

	ReviewContextBuilder(GitHubApi github, List<MissionCatalog> catalogs, FailedTestsSource failedTests,
			GitHubAppProperties app) {
		this(github, new StudentPlans(github, catalogs), failedTests, app);
	}

	@Autowired
	ReviewContextBuilder(GitHubApi github, StudentPlans plans, FailedTestsSource failedTests,
			GitHubAppProperties app) {
		this.github = github;
		this.plans = plans;
		this.failedTests = failedTests;
		this.app = app;
	}

	Optional<ReviewContext> build(String token, String repositoryFullName, int pullRequestNumber, String headSha) {
		Optional<StudentPlan> plan = plans.read(token, repositoryFullName);
		if (plan.isEmpty()) {
			return Optional.empty();
		}
		String diff = cap(github.pullRequestDiff(token, repositoryFullName, pullRequestNumber));
		var tests = failedTests.failedTests(token, repositoryFullName, headSha, plan.get());
		return Optional.of(new ReviewContext(token, repositoryFullName, pullRequestNumber, headSha, plan.get(), diff,
				tests.isPresent(), tests.orElse(List.of()), reviewerComments(token, repositoryFullName, pullRequestNumber)));
	}

	private List<PreviousComment> reviewerComments(String token, String repositoryFullName, int pullRequestNumber) {
		return github.issueComments(token, repositoryFullName, pullRequestNumber)
			.stream()
			.filter(GitHubComment::bot)
			.filter(comment -> app.botLogin().equals(comment.authorLogin()))
			.map(comment -> new PreviousComment(comment.id(), comment.body()))
			.toList();
	}

	private static String cap(String diff) {
		if (diff.length() <= MAX_DIFF_LENGTH) {
			return diff;
		}
		return diff.substring(0, MAX_DIFF_LENGTH) + "\n[... diff cortado em " + MAX_DIFF_LENGTH + " caracteres ...]\n";
	}

}
