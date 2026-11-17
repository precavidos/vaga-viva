package io.github.precavidos.vagaviva.review;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubFile;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the plan a student's repository carries in {@code .vaga-viva/plan.json}.
 * A repository without the file, or with a plan for an archetype Vaga Viva
 * does not know, is not a student repository: nothing is read for it.
 */
public final class StudentPlans {

	static final String PLAN_FILE = ".vaga-viva/plan.json";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final GitHubApi github;

	private final Map<String, MissionCatalog> catalogs;

	public StudentPlans(GitHubApi github, List<MissionCatalog> catalogs) {
		this.github = github;
		this.catalogs = catalogs.stream().collect(Collectors.toMap(MissionCatalog::archetype, Function.identity()));
	}

	public Optional<StudentPlan> read(String token, String repositoryFullName) {
		Optional<GitHubFile> file = github.readFile(token, repositoryFullName, PLAN_FILE);
		if (file.isEmpty()) {
			return Optional.empty();
		}
		JsonNode plan;
		try {
			plan = JSON.readTree(file.get().content());
		}
		catch (JacksonException e) {
			return Optional.empty();
		}
		MissionCatalog catalog = catalogs.get(plan.path("archetype").asString(""));
		if (catalog == null) {
			return Optional.empty();
		}
		var byId = catalog.missions().stream().collect(Collectors.toMap(Mission::id, Function.identity()));
		var missions = new ArrayList<Mission>();
		for (JsonNode id : plan.path("missions")) {
			Mission mission = byId.get(id.asString(""));
			if (mission != null && !missions.contains(mission)) {
				missions.add(mission);
			}
		}
		return Optional.of(new StudentPlan(catalog.archetype(), plan.path("domain").asString(catalog.domain().id()),
				missions));
	}

}
