package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubApiException;
import io.github.precavidos.vagaviva.github.GitHubComment;
import io.github.precavidos.vagaviva.github.GitHubPullRequest;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;

/**
 * Reads a student's repository as the Vaga Viva app, for the weekly e-mail.
 * Nothing is stored (ADR 0003).
 */
public class RepositorySnapshots {

	private final GitHubApi github;

	private final InstallationTokens tokens;

	private final StudentPlans plans;

	private final FailedTestsSource failedTests;

	private final GitHubAppProperties app;

	RepositorySnapshots(GitHubApi github, InstallationTokens tokens, StudentPlans plans, FailedTestsSource failedTests,
			GitHubAppProperties app) {
		this.github = github;
		this.tokens = tokens;
		this.plans = plans;
		this.failedTests = failedTests;
		this.app = app;
	}

	/** Whether the app's id and private key are set, so the repositories can be read at all. */
	public boolean enabled() {
		return !app.id().isBlank() && !app.privateKey().isBlank();
	}

	/**
	 * Empty when the app is not installed, the repository does not exist or it
	 * has no readable plan.
	 */
	public Optional<RepositorySnapshot> take(String repositoryFullName) {
		try {
			Optional<String> token = tokens.forRepository(repositoryFullName);
			if (token.isEmpty()) {
				return Optional.empty();
			}
			Optional<StudentPlan> plan = plans.read(token.get(), repositoryFullName);
			if (plan.isEmpty()) {
				return Optional.empty();
			}
			Optional<GitHubPullRequest> pullRequest = github.latestPullRequest(token.get(), repositoryFullName);
			String sha = pullRequest.map(GitHubPullRequest::headSha)
				.orElseGet(() -> github.latestCommitSha(token.get(), repositoryFullName));
			Optional<TestReport> report = failedTests.report(token.get(), repositoryFullName, sha);
			Optional<String> lastReview = pullRequest
				.flatMap(pr -> lastReview(token.get(), repositoryFullName, pr.number()));
			return Optional.of(new RepositorySnapshot(plan.get(), report, lastReview));
		}
		catch (GitHubApiException e) {
			if (e.status() == 404) {
				return Optional.empty();
			}
			throw e;
		}
	}

	private Optional<String> lastReview(String token, String repositoryFullName, int number) {
		List<GitHubComment> comments = github.issueComments(token, repositoryFullName, number);
		for (int i = comments.size() - 1; i >= 0; i--) {
			GitHubComment comment = comments.get(i);
			if (comment.bot() && app.botLogin().equals(comment.authorLogin())) {
				Optional<String> text = ReviewTexts.reviewText(comment.body());
				if (text.isPresent()) {
					return text;
				}
			}
		}
		return Optional.empty();
	}

}
