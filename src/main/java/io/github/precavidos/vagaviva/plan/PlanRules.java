package io.github.precavidos.vagaviva.plan;

/** Size and difficulty limits shared by the mission catalog and the planner. */
final class PlanRules {

	/** Every plan starts with this many core missions. */
	static final int CORE_MISSIONS = 3;

	static final int MIN_MISSIONS = 5;

	static final int MAX_MISSIONS = 6;

	static final int MIN_DIFFICULTY = 1;

	static final int MAX_DIFFICULTY = 4;

	/** Highest difficulty allowed for intern and junior postings, and when no level is recognized. */
	static final int JUNIOR_DIFFICULTY_CAP = 3;

	private PlanRules() {
	}

}
