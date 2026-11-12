package io.github.precavidos.vagaviva.classroom;

import java.time.Duration;
import java.time.Instant;

/**
 * How a student is doing, as the teacher's dashboard shows it.
 */
public enum StudentStatus {

	ADVANCING, STUCK, IDLE, COMPLETED;

	/**
	 * Decides the status. The rules apply in this order, so when both time limits hold, {@link #IDLE} wins over
	 * {@link #STUCK}.
	 * @param completed whether every planned mission passed
	 * @param currentMissionSince when the student reached the current mission
	 * @param lastActivityAt the student's last test report
	 * @param now the current time
	 * @param stuckAfter time on the same mission that makes a student stuck
	 * @param idleAfter time without activity that makes a student idle
	 */
	public static StudentStatus of(boolean completed, Instant currentMissionSince, Instant lastActivityAt, Instant now,
			Duration stuckAfter, Duration idleAfter) {
		if (completed) {
			return COMPLETED;
		}
		if (Duration.between(lastActivityAt, now).compareTo(idleAfter) >= 0) {
			return IDLE;
		}
		if (Duration.between(currentMissionSince, now).compareTo(stuckAfter) >= 0) {
			return STUCK;
		}
		return ADVANCING;
	}

}
