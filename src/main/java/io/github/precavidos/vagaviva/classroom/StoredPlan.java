package io.github.precavidos.vagaviva.classroom;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.precavidos.vagaviva.archetype.ArchetypeFit;
import io.github.precavidos.vagaviva.jobposting.SeniorityLevel;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.plan.PlannedMission;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import tools.jackson.databind.json.JsonMapper;

/**
 * A classroom's plan as it is kept in the database: ids only, so it can be
 * written as JSON without a word of the job posting (ADR 0003). The mission
 * texts and the fictional domain come back from the archetype's catalog.
 */
record StoredPlan(String archetype, ArchetypeFit archetypeFit, SeniorityLevel seniorityLevel, int difficultyCap,
		List<StoredMission> missions, List<String> postingSkills, List<String> postingTechnologies) {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	StoredPlan {
		missions = List.copyOf(missions);
		postingSkills = List.copyOf(postingSkills);
		postingTechnologies = List.copyOf(postingTechnologies);
	}

	record StoredMission(String id, PlannedMission.Reason reason, List<String> matchedSkills) {

		StoredMission {
			matchedSkills = List.copyOf(matchedSkills);
		}

	}

	static StoredPlan of(ProjectPlan plan) {
		var missions = plan.missions()
			.stream()
			.map((m) -> new StoredMission(m.mission().id(), m.reason(), m.matchedSkills()))
			.toList();
		return new StoredPlan(plan.archetype(), plan.archetypeFit(), plan.seniorityLevel(), plan.difficultyCap(),
				missions, plan.postingSkills(), plan.postingTechnologies());
	}

	/** Rebuilds the plan with the missions and the domain of the archetype's catalog. */
	ProjectPlan toProjectPlan(MissionCatalog catalog) {
		if (!catalog.archetype().equals(archetype)) {
			throw new IllegalArgumentException("The catalog is not the plan's archetype: " + archetype);
		}
		Map<String, Mission> byId = catalog.missions()
			.stream()
			.collect(Collectors.toMap(Mission::id, Function.identity()));
		var planned = missions.stream().map((stored) -> {
			Mission mission = byId.get(stored.id());
			if (mission == null) {
				throw new IllegalStateException("The catalog no longer has the mission " + stored.id());
			}
			return new PlannedMission(mission, stored.reason(), stored.matchedSkills());
		}).toList();
		return new ProjectPlan(archetype, archetypeFit, catalog.domain(), seniorityLevel, difficultyCap, planned,
				postingSkills, postingTechnologies);
	}

	List<String> missionIds() {
		return missions.stream().map(StoredMission::id).toList();
	}

	String toJson() {
		return JSON.writeValueAsString(this);
	}

	static StoredPlan fromJson(String json) {
		return JSON.readValue(json, StoredPlan.class);
	}

}
