package io.github.precavidos.vagaviva.review;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubApiException;
import io.github.precavidos.vagaviva.github.GitHubArtifact;
import io.github.precavidos.vagaviva.github.GitHubWorkflowRun;

/**
 * Finds the newest completed {@value WebhookDispatcher#WORKFLOW} run for a
 * commit, downloads its JUnit artifact and reads it into a {@link TestReport}. When there is no finished
 * run or readable report, the answer is empty: the reviewer then knows it
 * cannot tell which tests fail.
 */
@Component
class JunitFailedTests implements FailedTestsSource {

	/** The artifacts {@code vaga-viva.yml} uploads: Surefire's (Java) and jest-junit's (TypeScript). */
	static final Set<String> REPORT_ARTIFACTS = Set.of("surefire-reports", "junit-report");

	/** A compressed report is a few kilobytes; anything far larger is not a report. */
	static final long MAX_ARTIFACT_BYTES = 5L * 1024 * 1024;

	private static final Logger log = LoggerFactory.getLogger(JunitFailedTests.class);

	private final GitHubApi github;

	JunitFailedTests(GitHubApi github) {
		this.github = github;
	}

	@Override
	public Optional<TestReport> report(String token, String repositoryFullName, String headSha) {
		try {
			Optional<GitHubWorkflowRun> run = github.workflowRuns(token, repositoryFullName, headSha)
				.stream()
				.filter(r -> WebhookDispatcher.WORKFLOW.equals(r.name()) && r.completed())
				.findFirst();
			if (run.isEmpty()) {
				return Optional.empty();
			}
			Optional<GitHubArtifact> artifact = github.runArtifacts(token, repositoryFullName, run.get().id())
				.stream()
				.filter(a -> REPORT_ARTIFACTS.contains(a.name()) && !a.expired() && a.sizeInBytes() <= MAX_ARTIFACT_BYTES)
				.findFirst();
			if (artifact.isEmpty()) {
				return Optional.empty();
			}
			byte[] zip = github.downloadArtifact(token, repositoryFullName, artifact.get().id());
			return Optional.of(JunitReports.read(zip));
		}
		catch (GitHubApiException | IllegalArgumentException e) {
			log.warn("Could not read the test report of {} @ {}: {}", repositoryFullName, headSha,
					e instanceof GitHubApiException g ? "status " + g.status() : e.getMessage());
			return Optional.empty();
		}
	}

}
