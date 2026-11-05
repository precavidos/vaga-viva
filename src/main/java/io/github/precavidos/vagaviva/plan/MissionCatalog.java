package io.github.precavidos.vagaviva.plan;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;
import tools.jackson.databind.json.JsonMapper;

/**
 * All missions an archetype's template repository offers, its fixed fictional
 * domain and the missions used to fill short plans. It only exists in a valid
 * state, checked against the archetype's skill vocabulary.
 */
public final class MissionCatalog {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final String archetype;

	private final FictionalDomain domain;

	private final Map<String, Mission> missions;

	private final List<Mission> defaultMissions;

	private MissionCatalog(CatalogFile file, SkillVocabulary vocabulary) {
		this.archetype = file.archetype();
		this.domain = file.domain();
		var problems = new ArrayList<String>();
		if (!vocabulary.archetype().equals(archetype)) {
			problems.add("catalog archetype " + archetype + " does not match vocabulary " + vocabulary.archetype());
		}
		if (domain == null || domain.id() == null || domain.name() == null) {
			problems.add("catalog has no fictional domain");
		}
		this.missions = index(file.missions(), vocabulary, problems);
		this.defaultMissions = resolveDefaults(file.defaultMissions(), problems);
		if (!problems.isEmpty()) {
			throw new InvalidMissionCatalogException(archetype, problems);
		}
	}

	public static MissionCatalog read(InputStream input, SkillVocabulary vocabulary) {
		return new MissionCatalog(JSON.readValue(input, CatalogFile.class), vocabulary);
	}

	public static MissionCatalog fromClasspath(String path, SkillVocabulary vocabulary) {
		try (InputStream input = MissionCatalog.class.getClassLoader().getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalArgumentException("Mission catalog not found on the classpath: " + path);
			}
			return read(input, vocabulary);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public String archetype() {
		return archetype;
	}

	public FictionalDomain domain() {
		return domain;
	}

	/** All missions, in catalog order. */
	public List<Mission> missions() {
		return List.copyOf(missions.values());
	}

	/** Optional missions used, in this order, to fill plans that matched too few skills. */
	public List<Mission> defaultMissions() {
		return defaultMissions;
	}

	private static Map<String, Mission> index(List<Mission> list, SkillVocabulary vocabulary, List<String> problems) {
		var index = new LinkedHashMap<String, Mission>();
		int coreCount = 0;
		for (Mission mission : list == null ? List.<Mission>of() : list) {
			if (index.putIfAbsent(mission.id(), mission) != null) {
				problems.add("duplicate mission: " + mission.id());
				continue;
			}
			if (mission.skills().isEmpty()) {
				problems.add("mission " + mission.id() + " has no skills");
			}
			for (String skill : mission.skills()) {
				if (vocabulary.skill(skill).isEmpty()) {
					problems.add("mission " + mission.id() + " points to an unknown skill: " + skill);
				}
			}
			if (mission.difficulty() < PlanRules.MIN_DIFFICULTY || mission.difficulty() > PlanRules.MAX_DIFFICULTY) {
				problems.add("mission " + mission.id() + " has a difficulty outside " + PlanRules.MIN_DIFFICULTY + "-"
						+ PlanRules.MAX_DIFFICULTY + ": " + mission.difficulty());
			}
			if (mission.core()) {
				coreCount++;
				if (mission.difficulty() > PlanRules.JUNIOR_DIFFICULTY_CAP) {
					problems.add("core mission " + mission.id() + " is harder than the junior cap of "
							+ PlanRules.JUNIOR_DIFFICULTY_CAP);
				}
			}
		}
		if (coreCount != PlanRules.CORE_MISSIONS) {
			problems.add("catalog needs " + PlanRules.CORE_MISSIONS + " core missions, found " + coreCount);
		}
		return index;
	}

	private List<Mission> resolveDefaults(List<String> ids, List<String> problems) {
		var defaults = new ArrayList<Mission>();
		for (String id : ids == null ? List.<String>of() : ids) {
			Mission mission = missions.get(id);
			if (mission == null) {
				problems.add("default mission " + id + " is unknown");
			}
			else if (mission.core()) {
				problems.add("default mission " + id + " is core");
			}
			else if (mission.difficulty() > PlanRules.JUNIOR_DIFFICULTY_CAP) {
				problems.add("default mission " + id + " is harder than the junior cap of "
						+ PlanRules.JUNIOR_DIFFICULTY_CAP);
			}
			else {
				defaults.add(mission);
			}
		}
		int needed = PlanRules.MIN_MISSIONS - PlanRules.CORE_MISSIONS;
		if (ids == null || ids.size() < needed) {
			problems.add("catalog needs at least " + needed + " default missions, found " + (ids == null ? 0 : ids.size()));
		}
		return List.copyOf(defaults);
	}

	private record CatalogFile(String archetype, FictionalDomain domain, List<Mission> missions,
			List<String> defaultMissions) {
	}

}
