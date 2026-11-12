package io.github.precavidos.vagaviva.classroom;

import java.util.List;

import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.TestReport;

/**
 * Where a student stands in the classroom's plan, according to one test report.
 */
public sealed interface MissionProgress {

	/**
	 * Decides the progress from a report.
	 * <p>
	 * A report that lacks the tests of any planned mission (a broken build, an edited {@code plan.json}) says nothing
	 * reliable, so it is {@link Unknown}. Failures of missions outside the plan are ignored.
	 * @param planMissionIds the classroom's missions, in plan order
	 * @param report what the student's test run reported
	 */
	static MissionProgress from(List<String> planMissionIds, TestReport report) {
		if (!report.missionsRun().containsAll(planMissionIds)) {
			return new Unknown();
		}
		for (String missionId : planMissionIds) {
			List<FailedTest> failing = report.failed().stream().filter(f -> f.missionId().equals(missionId)).toList();
			if (!failing.isEmpty()) {
				return new OnMission(missionId, failing);
			}
		}
		return new Completed();
	}

	/** The first mission of the plan that still has failing tests. */
	record OnMission(String missionId, List<FailedTest> failing) implements MissionProgress {

		public OnMission {
			failing = List.copyOf(failing);
		}

	}

	/** Every planned mission ran and passed. */
	record Completed() implements MissionProgress {
	}

	/** A partial report: it changes nothing about the student's mission. */
	record Unknown() implements MissionProgress {
	}

}
