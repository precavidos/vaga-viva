package io.github.precavidos.vagaviva.classroom;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.TestReport;

import static org.assertj.core.api.Assertions.assertThat;

class MissionProgressTests {

	private static final List<String> PLAN = List.of("patients-api", "persistence", "pagination");

	@Test
	void firstFailingMissionInPlanOrder() {
		FailedTest paginationFailure = new FailedTest("pagination", "returnsSecondPage", "expected 2");
		FailedTest persistenceFailure = new FailedTest("persistence", "savesPatient", "expected saved");
		FailedTest otherPersistenceFailure = new FailedTest("persistence", "findsPatient", "expected found");
		TestReport report = new TestReport(Set.copyOf(PLAN),
				List.of(paginationFailure, persistenceFailure, otherPersistenceFailure));

		assertThat(MissionProgress.from(PLAN, report))
			.isEqualTo(new MissionProgress.OnMission("persistence", List.of(persistenceFailure, otherPersistenceFailure)));
	}

	@Test
	void completedWhenEveryPlannedMissionRanAndPassed() {
		assertThat(MissionProgress.from(PLAN, new TestReport(Set.copyOf(PLAN), List.of())))
			.isEqualTo(new MissionProgress.Completed());
	}

	@Test
	void unknownWhenAPlannedMissionDidNotRun() {
		TestReport report = new TestReport(Set.of("patients-api", "persistence"), List.of());

		assertThat(MissionProgress.from(PLAN, report)).isEqualTo(new MissionProgress.Unknown());
	}

	@Test
	void missionsOutsideThePlanAreIgnored() {
		FailedTest outsider = new FailedTest("bug-hunt", "findsTheBug", "expected fixed");
		TestReport report = new TestReport(Set.of("patients-api", "persistence", "pagination", "bug-hunt"),
				List.of(outsider));

		assertThat(MissionProgress.from(PLAN, report)).isEqualTo(new MissionProgress.Completed());
	}

}
