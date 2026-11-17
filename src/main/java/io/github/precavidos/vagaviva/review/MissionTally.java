package io.github.precavidos.vagaviva.review;

/**
 * How many of a mission's tests ran and how many of those passed.
 *
 * @param passed the tests that ran without a failure or error
 * @param total the tests that ran (skipped tests do not count)
 */
public record MissionTally(int passed, int total) {

	MissionTally plus(MissionTally other) {
		return new MissionTally(passed + other.passed, total + other.total);
	}

}
