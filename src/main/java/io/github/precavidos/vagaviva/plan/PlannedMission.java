package io.github.precavidos.vagaviva.plan;

import java.util.List;

/**
 * A mission chosen for a plan, why it was chosen, and which of the posting's
 * skills it exercises (in the mission's own skill order).
 */
public record PlannedMission(Mission mission, Reason reason, List<String> matchedSkills) {

	public enum Reason {

		/** Every plan includes it. */
		CORE,

		/** Chosen because it exercises skills the posting asks for. */
		SKILL_MATCH,

		/** Fills a plan that matched too few skills. */
		DEFAULT

	}

	public PlannedMission {
		matchedSkills = List.copyOf(matchedSkills);
	}

}
