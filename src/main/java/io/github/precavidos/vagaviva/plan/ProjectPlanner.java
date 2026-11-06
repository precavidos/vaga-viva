package io.github.precavidos.vagaviva.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import io.github.precavidos.vagaviva.jobposting.ExtractedJobPosting;
import io.github.precavidos.vagaviva.jobposting.SeniorityLevel;
import io.github.precavidos.vagaviva.plan.PlannedMission.Reason;

/**
 * Chooses and orders a posting's missions with fixed rules, so the same
 * posting always gets the same plan (ADR 0002):
 * <ol>
 * <li>every plan gets the core missions;</li>
 * <li>up to three optional missions within the difficulty cap are added, those
 * matching the most posting skills first, ties broken by catalog order;</li>
 * <li>plans still under the minimum size are filled with the default
 * missions;</li>
 * <li>missions are ordered by difficulty, core first, then catalog order.</li>
 * </ol>
 * The difficulty cap comes from the lowest level the posting names; intern,
 * junior and unknown levels are capped lower than mid and senior.
 */
public class ProjectPlanner {

	private static final int MID_AND_SENIOR_DIFFICULTY_CAP = PlanRules.MAX_DIFFICULTY;

	private final MissionCatalog catalog;

	public ProjectPlanner(MissionCatalog catalog) {
		this.catalog = catalog;
	}

	public ProjectPlan plan(ExtractedJobPosting posting) {
		var skills = posting.recognition().skills().stream().map(s -> s.skill().id()).toList();
		var technologies = posting.recognition().technologies().stream().map(t -> t.technology().id()).toList();
		return plan(posting.seniorityLevels(), skills, technologies);
	}

	public ProjectPlan plan(List<SeniorityLevel> postingLevels, Collection<String> postingSkills) {
		return plan(postingLevels, postingSkills, List.of());
	}

	public ProjectPlan plan(List<SeniorityLevel> postingLevels, Collection<String> postingSkills,
			Collection<String> postingTechnologies) {
		SeniorityLevel level = postingLevels.stream().min(Comparator.naturalOrder()).orElse(SeniorityLevel.JUNIOR);
		int cap = difficultyCap(level);
		Set<String> skills = Set.copyOf(postingSkills);

		var chosen = new ArrayList<PlannedMission>();
		catalog.missions()
			.stream()
			.filter(Mission::core)
			.forEach(m -> chosen.add(new PlannedMission(m, Reason.CORE, matched(m, skills))));

		catalog.missions()
			.stream()
			.filter(m -> !m.core() && m.difficulty() <= cap)
			.map(m -> new PlannedMission(m, Reason.SKILL_MATCH, matched(m, skills)))
			.filter(m -> !m.matchedSkills().isEmpty())
			.sorted(Comparator.comparingInt((PlannedMission m) -> m.matchedSkills().size()).reversed()
				.thenComparing(m -> catalogIndex(m.mission())))
			.limit(PlanRules.MAX_MISSIONS - PlanRules.CORE_MISSIONS)
			.forEach(chosen::add);

		for (Mission fallback : catalog.defaultMissions()) {
			if (chosen.size() >= PlanRules.MIN_MISSIONS) {
				break;
			}
			if (chosen.stream().noneMatch(m -> m.mission().equals(fallback))) {
				chosen.add(new PlannedMission(fallback, Reason.DEFAULT, matched(fallback, skills)));
			}
		}

		chosen.sort(Comparator.comparingInt((PlannedMission m) -> m.mission().difficulty())
			.thenComparing(m -> !m.mission().core())
			.thenComparing(m -> catalogIndex(m.mission())));
		return new ProjectPlan(catalog.archetype(), catalog.domain(), level, cap, chosen, List.copyOf(postingSkills),
				List.copyOf(postingTechnologies));
	}

	private static int difficultyCap(SeniorityLevel level) {
		return switch (level) {
			case INTERN, JUNIOR -> PlanRules.JUNIOR_DIFFICULTY_CAP;
			case MID, SENIOR -> MID_AND_SENIOR_DIFFICULTY_CAP;
		};
	}

	private static List<String> matched(Mission mission, Set<String> skills) {
		return mission.skills().stream().filter(skills::contains).toList();
	}

	private int catalogIndex(Mission mission) {
		return catalog.missions().indexOf(mission);
	}

}
