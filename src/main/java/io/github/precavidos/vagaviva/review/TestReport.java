package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;

/**
 * What a student's JUnit report says about the missions' tests.
 *
 * @param missionsRun the ids of the missions with at least one test that ran (skipped tests do not count)
 * @param failed every failed mission test, in report order, whether or not it is in the student's plan
 * @param tallies passed and total tests per mission id, only for missions with at least one test that ran
 */
public record TestReport(Set<String> missionsRun, List<FailedTest> failed, Map<String, MissionTally> tallies) {

	public TestReport {
		missionsRun = Set.copyOf(missionsRun);
		failed = List.copyOf(failed);
		tallies = Map.copyOf(tallies);
	}

	public TestReport(Set<String> missionsRun, List<FailedTest> failed) {
		this(missionsRun, failed, Map.of());
	}

}
