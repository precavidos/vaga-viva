package io.github.precavidos.vagaviva.plan;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.github.precavidos.vagaviva.jobposting.ExtractedJobPosting;

/** Plans a posting with the mission catalog of the archetype chosen for it (ADR 0006). */
public final class ProjectPlanners {

	private final Map<String, ProjectPlanner> byArchetype;

	public ProjectPlanners(List<MissionCatalog> catalogs) {
		this.byArchetype = catalogs.stream()
			.collect(Collectors.toMap(MissionCatalog::archetype, ProjectPlanner::new, (a, b) -> {
				throw new IllegalArgumentException("Two mission catalogs for the same archetype");
			}));
	}

	public ProjectPlan plan(ExtractedJobPosting posting) {
		return forArchetype(posting.archetype()).plan(posting);
	}

	public ProjectPlanner forArchetype(String archetype) {
		ProjectPlanner planner = byArchetype.get(archetype);
		if (planner == null) {
			throw new IllegalArgumentException("No mission catalog for archetype " + archetype);
		}
		return planner;
	}

}
