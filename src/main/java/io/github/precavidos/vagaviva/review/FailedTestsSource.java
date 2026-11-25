package io.github.precavidos.vagaviva.review;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;

/**
 * Reads which of the plan's tests failed on a commit, from the workflow's
 * JUnit report (#23). Empty when no finished run or readable report exists yet,
 * which is not the same as "no test failed".
 */
public interface FailedTestsSource {

	/** The whole report of the commit, not narrowed to any plan. */
	Optional<TestReport> report(String token, String repositoryFullName, String headSha);

	/** The plan's failed tests on the commit, in plan order. */
	default Optional<List<FailedTest>> failedTests(String token, String repositoryFullName, String headSha,
			StudentPlan plan) {
		return report(token, repositoryFullName, headSha).map(report -> inPlanOrder(report, plan));
	}

	/** The report's failed tests of the plan's missions, in plan order. */
	static List<FailedTest> inPlanOrder(TestReport report, StudentPlan plan) {
		List<String> order = plan.missions().stream().map(Mission::id).toList();
		return report.failed()
			.stream()
			.filter(test -> order.contains(test.missionId()))
			.sorted(Comparator.comparingInt(test -> order.indexOf(test.missionId())))
			.toList();
	}

	/** The report's tallies of the plan's missions only. */
	static Map<String, MissionTally> planTallies(TestReport report, StudentPlan plan) {
		var tallies = new LinkedHashMap<String, MissionTally>();
		plan.missions().forEach(mission -> {
			MissionTally tally = report.tallies().get(mission.id());
			if (tally != null) {
				tallies.put(mission.id(), tally);
			}
		});
		return tallies;
	}

}
