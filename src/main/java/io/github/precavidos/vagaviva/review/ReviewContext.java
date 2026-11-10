package io.github.precavidos.vagaviva.review;

import java.util.List;

import io.github.precavidos.vagaviva.plan.Mission;

/**
 * Everything the reviewer (#24) and the hints (#27) need about one pull
 * request, read in memory from GitHub. Nothing here is stored (ADR 0003).
 *
 * @param token an installation token that acts as the app on the repository
 * @param plan the student's plan, from {@code .vaga-viva/plan.json}
 * @param diff the pull request's diff, capped at {@link ReviewContextBuilder#MAX_DIFF_LENGTH} characters
 * @param testReportRead whether the workflow's test report was found and read; when
 * {@code false}, {@code failedTests} is empty because nothing is known yet, not because
 * every test passed
 * @param failedTests the plan's failed tests on the pull request's head commit, in plan order
 * @param reviewerComments the app's own earlier comments on the pull request, oldest first
 */
public record ReviewContext(String token, String repositoryFullName, int pullRequestNumber, String headSha,
		StudentPlan plan, String diff, boolean testReportRead, List<FailedTest> failedTests,
		List<PreviousComment> reviewerComments) {

	public ReviewContext {
		failedTests = List.copyOf(failedTests);
		reviewerComments = List.copyOf(reviewerComments);
	}

	/** Never shows the token or the diff. */
	@Override
	public String toString() {
		return "ReviewContext[" + repositoryFullName + "#" + pullRequestNumber + " @ " + headSha + ", testReportRead="
				+ testReportRead + ", failedTests=" + failedTests.size() + "]";
	}

	/** The plan written in the student's repository, with the missions resolved from the archetype's catalog. */
	public record StudentPlan(String archetype, String domain, List<Mission> missions) {

		public StudentPlan {
			missions = List.copyOf(missions);
		}

	}

	/** A failed test of a mission in the plan; {@code message} is the test's Portuguese assertion text. */
	public record FailedTest(String missionId, String testName, String message) {
	}

	/** A comment the app already posted on the pull request. */
	public record PreviousComment(long id, String body) {
	}

}
