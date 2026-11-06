package io.github.precavidos.vagaviva.plan;

import java.util.List;

import io.github.precavidos.vagaviva.jobposting.SeniorityLevel;

/**
 * The fictional domain and the ordered missions chosen for a job posting.
 * {@code seniorityLevel} is the level the plan was built for, which may be
 * assumed when the posting names none. {@code postingSkills} and
 * {@code postingTechnologies} are the ids recognized in the posting, in the
 * order received; they carry no words from the posting itself.
 */
public record ProjectPlan(String archetype, FictionalDomain domain, SeniorityLevel seniorityLevel, int difficultyCap,
		List<PlannedMission> missions, List<String> postingSkills, List<String> postingTechnologies) {

	public ProjectPlan {
		missions = List.copyOf(missions);
		postingSkills = List.copyOf(postingSkills);
		postingTechnologies = List.copyOf(postingTechnologies);
	}

}
