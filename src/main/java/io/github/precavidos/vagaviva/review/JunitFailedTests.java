package io.github.precavidos.vagaviva.review;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubApiException;
import io.github.precavidos.vagaviva.github.GitHubArtifact;
import io.github.precavidos.vagaviva.github.GitHubWorkflowRun;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;

/**
 * Finds the newest completed {@value WebhookDispatcher#WORKFLOW} run for a
 * commit, downloads its JUnit artifact and keeps the failed tests of the
 * missions in the student's plan, in plan order. When there is no finished
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
	public Optional<List<FailedTest>> failedTests(String token, String repositoryFullName, String headSha,
			StudentPlan plan) {
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
			return Optional.of(inPlan(JunitReports.failedTests(zip), plan));
		}
		catch (GitHubApiException | IllegalArgumentException e) {
			log.warn("Could not read the test report of {} @ {}: {}", repositoryFullName, headSha,
					e instanceof GitHubApiException g ? "status " + g.status() : e.getMessage());
			return Optional.empty();
		}
	}

	private static List<FailedTest> inPlan(List<FailedTest> failed, StudentPlan plan) {
		List<String> order = plan.missions().stream().map(Mission::id).toList();
		return failed.stream()
			.filter(test -> order.contains(test.missionId()))
			.sorted(Comparator.comparingInt(test -> order.indexOf(test.missionId())))
			.toList();
	}

}
