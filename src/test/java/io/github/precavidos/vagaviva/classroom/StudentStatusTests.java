package io.github.precavidos.vagaviva.classroom;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudentStatusTests {

	private static final Instant NOW = Instant.parse("2026-11-20T12:00:00Z");

	private static final Duration STUCK_AFTER = Duration.ofDays(3);

	private static final Duration IDLE_AFTER = Duration.ofDays(5);

	private static StudentStatus status(boolean completed, Duration onMission, Duration sinceActivity) {
		return StudentStatus.of(completed, NOW.minus(onMission), NOW.minus(sinceActivity), NOW, STUCK_AFTER,
				IDLE_AFTER);
	}

	@Test
	void idleWinsOverStuck() {
		assertThat(status(false, Duration.ofDays(6), Duration.ofDays(5))).isEqualTo(StudentStatus.IDLE);
	}

	@Test
	void stuckAfterThreeDaysOnTheMission() {
		assertThat(status(false, Duration.ofDays(3), Duration.ofHours(1))).isEqualTo(StudentStatus.STUCK);
	}

	@Test
	void advancingBeforeThreeDays() {
		assertThat(status(false, Duration.ofDays(2).plusHours(23), Duration.ofHours(1)))
			.isEqualTo(StudentStatus.ADVANCING);
	}

	@Test
	void completedIgnoresTime() {
		assertThat(status(true, Duration.ofDays(30), Duration.ofDays(30))).isEqualTo(StudentStatus.COMPLETED);
	}

}
