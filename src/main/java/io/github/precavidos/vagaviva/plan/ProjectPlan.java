package io.github.precavidos.vagaviva.plan;

import java.util.List;

import io.github.precavidos.vagaviva.jobposting.SeniorityLevel;

/**
 * The fictional domain and the ordered missions chosen for a job posting.
 * {@code seniorityLevel} is the level the plan was built for, which may be
 * assumed when the posting names none.
 */
public record ProjectPlan(String archetype, FictionalDomain domain, SeniorityLevel seniorityLevel, int difficultyCap,
		List<PlannedMission> missions) {

	public ProjectPlan {
		missions = List.copyOf(missions);
	}

}
