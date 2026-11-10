package io.github.precavidos.vagaviva.review;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubComment;
import io.github.precavidos.vagaviva.github.GitHubFile;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.review.ReviewContext.PreviousComment;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads, with an installation token, what the reviewer needs about a pull
 * request. A repository without {@code .vaga-viva/plan.json}, or with a plan
 * for an archetype Vaga Viva does not know, is not a student repository:
 * nothing is built for it.
 */
@Component
class ReviewContextBuilder {

	static final String PLAN_FILE = ".vaga-viva/plan.json";

	/** Enough for a mission's change; a larger diff is cut, with a marker. */
	static final int MAX_DIFF_LENGTH = 60_000;

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final GitHubApi github;

	private final Map<String, MissionCatalog> catalogs;

	private final FailedTestsSource failedTests;

	private final GitHubAppProperties app;

	ReviewContextBuilder(GitHubApi github, List<MissionCatalog> catalogs, FailedTestsSource failedTests,
			GitHubAppProperties app) {
		this.github = github;
		this.catalogs = catalogs.stream().collect(Collectors.toMap(MissionCatalog::archetype, Function.identity()));
		this.failedTests = failedTests;
		this.app = app;
	}

	Optional<ReviewContext> build(String token, String repositoryFullName, int pullRequestNumber, String headSha) {
		Optional<StudentPlan> plan = readPlan(token, repositoryFullName);
		if (plan.isEmpty()) {
			return Optional.empty();
		}
		String diff = cap(github.pullRequestDiff(token, repositoryFullName, pullRequestNumber));
		var tests = failedTests.failedTests(token, repositoryFullName, headSha, plan.get());
		return Optional.of(new ReviewContext(token, repositoryFullName, pullRequestNumber, headSha, plan.get(), diff,
				tests.isPresent(), tests.orElse(List.of()), reviewerComments(token, repositoryFullName, pullRequestNumber)));
	}

	private Optional<StudentPlan> readPlan(String token, String repositoryFullName) {
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

	private List<PreviousComment> reviewerComments(String token, String repositoryFullName, int pullRequestNumber) {
		return github.issueComments(token, repositoryFullName, pullRequestNumber)
			.stream()
			.filter(GitHubComment::bot)
			.filter(comment -> app.botLogin().equals(comment.authorLogin()))
			.map(comment -> new PreviousComment(comment.id(), comment.body()))
			.toList();
	}

	private static String cap(String diff) {
		if (diff.length() <= MAX_DIFF_LENGTH) {
			return diff;
		}
		return diff.substring(0, MAX_DIFF_LENGTH) + "\n[... diff cortado em " + MAX_DIFF_LENGTH + " caracteres ...]\n";
	}

}
