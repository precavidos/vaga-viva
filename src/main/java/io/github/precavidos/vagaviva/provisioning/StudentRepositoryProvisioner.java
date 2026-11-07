package io.github.precavidos.vagaviva.provisioning;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubApiException;
import io.github.precavidos.vagaviva.github.GitHubIssue;
import io.github.precavidos.vagaviva.github.GitHubRepository;
import io.github.precavidos.vagaviva.plan.PlannedMission;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;
import io.github.precavidos.vagaviva.writing.MissionTextWriter;
import io.github.precavidos.vagaviva.writing.WrittenTexts;
import tools.jackson.databind.json.JsonMapper;

/**
 * Creates a student's repository in their own account from the template,
 * writes the plan file that switches off unplanned missions (ADR 0005) and
 * opens one issue per planned mission, in plan order.
 */
@Service
public class StudentRepositoryProvisioner {

	private static final Logger log = LoggerFactory.getLogger(StudentRepositoryProvisioner.class);

	private static final String README_FILE = "README.md";

	static final String PLAN_FILE = ".vaga-viva/plan.json";

	private static final int MAX_NAME_ATTEMPTS = 5;

	private static final int MAX_READY_ATTEMPTS = 5;

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final GitHubApi github;

	private final SkillVocabulary vocabulary;

	private final MissionTextWriter writer;

	private final Pause pause;

	@Autowired
	StudentRepositoryProvisioner(GitHubApi github, SkillVocabulary vocabulary, MissionTextWriter writer) {
		this(github, vocabulary, writer, Pause.SLEEP);
	}

	StudentRepositoryProvisioner(GitHubApi github, SkillVocabulary vocabulary, MissionTextWriter writer,
			Pause pause) {
		this.github = github;
		this.vocabulary = vocabulary;
		this.writer = writer;
		this.pause = pause;
	}

	public ProvisionedRepository provision(String token, String studentLogin, ProjectPlan plan) {
		WrittenTexts texts = writeTexts(plan);
		GitHubRepository repository = generate(token, studentLogin, plan);
		whenReady(() -> {
			github.createFile(token, repository.fullName(), PLAN_FILE, "Add Vaga Viva project plan", planFile(plan));
			return null;
		});
		var issues = new ArrayList<MissionIssue>();
		List<PlannedMission> missions = plan.missions();
		for (int i = 0; i < missions.size(); i++) {
			var planned = missions.get(i);
			var mission = planned.mission();
			String context = Optional.ofNullable(texts.missionContexts().get(mission.id()))
				.orElseGet(() -> DefaultStudentTexts.missionContext(planned, vocabulary));
			GitHubIssue issue = github.createIssue(token, repository.fullName(), MissionIssueText.title(i + 1, mission),
					MissionIssueText.body(mission, context, vocabulary));
			issues.add(new MissionIssue(mission.id(), issue.number(), issue.htmlUrl()));
		}
		String intro = texts.projectIntro().orElseGet(() -> DefaultStudentTexts.projectIntro(plan, vocabulary));
		String section = StudentReadme.projectSection(intro, missions, issues);
		try {
			whenReady(() -> {
				addProjectSection(token, repository.fullName(), plan, section);
				return null;
			});
		}
		catch (RuntimeException e) {
			// The section is cosmetic: every issue already carries its context.
			log.warn("Could not add the project section to the README ({}); provisioning continues",
					e instanceof GitHubApiException g ? "status " + g.status() : e.getClass().getSimpleName());
		}
		return new ProvisionedRepository(repository.fullName(), repository.htmlUrl(), issues);
	}

	/** The LLM never makes provisioning fail: any problem means default texts (ADR 0002). */
	private WrittenTexts writeTexts(ProjectPlan plan) {
		try {
			WrittenTexts texts = writer.write(plan);
			return texts != null ? texts : WrittenTexts.none();
		}
		catch (RuntimeException e) {
			log.warn("Could not write the student texts ({}); using the default texts", e.getClass().getSimpleName());
			return WrittenTexts.none();
		}
	}

	private void addProjectSection(String token, String repositoryFullName, ProjectPlan plan, String section) {
		String message = "Add your project section to the README";
		var readme = github.readFile(token, repositoryFullName, README_FILE);
		if (readme.isPresent()) {
			github.updateFile(token, repositoryFullName, README_FILE, message,
					StudentReadme.withProjectSection(readme.get().content(), section), readme.get().sha());
		}
		else {
			github.createFile(token, repositoryFullName, README_FILE, message,
					StudentReadme.newReadme(plan.domain(), section));
		}
	}

	private GitHubRepository generate(String token, String owner, ProjectPlan plan) {
		String baseName = "vaga-viva-" + plan.domain().id();
		String description = "Projeto de estudo do Vaga Viva: " + plan.domain().name();
		for (int attempt = 1;; attempt++) {
			String name = attempt == 1 ? baseName : baseName + "-" + attempt;
			try {
				return github.generateFromTemplate(token, owner, name, description);
			}
			catch (GitHubApiException e) {
				boolean nameTaken = e.status() == 422;
				if (!nameTaken || attempt == MAX_NAME_ATTEMPTS) {
					throw e;
				}
			}
		}
	}

	/**
	 * GitHub creates repositories from templates asynchronously, so the first
	 * writes may get 404 or 409 for a moment. Waits 1s, 2s, 4s... between tries.
	 */
	private <T> T whenReady(Supplier<T> call) {
		Duration wait = Duration.ofSeconds(1);
		for (int attempt = 1;; attempt++) {
			try {
				return call.get();
			}
			catch (GitHubApiException e) {
				boolean notReadyYet = e.status() == 404 || e.status() == 409;
				if (!notReadyYet || attempt == MAX_READY_ATTEMPTS) {
					throw e;
				}
				pause.pause(wait);
				wait = wait.multipliedBy(2);
			}
		}
	}

	private static String planFile(ProjectPlan plan) {
		var content = new LinkedHashMap<String, Object>();
		content.put("archetype", plan.archetype());
		content.put("domain", plan.domain().id());
		content.put("missions", plan.missions().stream().map(m -> m.mission().id()).toList());
		return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(content) + "\n";
	}

	/** Waits between retries; replaced in tests. */
	@FunctionalInterface
	interface Pause {

		Pause SLEEP = duration -> {
			try {
				Thread.sleep(duration);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while waiting for GitHub", e);
			}
		};

		void pause(Duration duration);

	}

}
