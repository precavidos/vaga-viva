package io.github.precavidos.vagaviva.provisioning;

import io.github.precavidos.vagaviva.vocabulary.Vocabularies;
import io.github.precavidos.vagaviva.archetype.ArchetypeCatalog;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubApiException;
import io.github.precavidos.vagaviva.github.GitHubArtifact;
import io.github.precavidos.vagaviva.github.GitHubWorkflowRun;
import io.github.precavidos.vagaviva.github.GitHubComment;
import io.github.precavidos.vagaviva.github.GitHubPullRequest;
import io.github.precavidos.vagaviva.github.InstallationToken;
import io.github.precavidos.vagaviva.github.GitHubFile;
import io.github.precavidos.vagaviva.github.GitHubIssue;
import io.github.precavidos.vagaviva.github.GitHubRepository;
import io.github.precavidos.vagaviva.github.GitHubUser;
import io.github.precavidos.vagaviva.jobposting.SeniorityLevel;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import io.github.precavidos.vagaviva.plan.ProjectPlanner;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;
import io.github.precavidos.vagaviva.writing.MissionTextWriter;
import io.github.precavidos.vagaviva.writing.WrittenTexts;

class StudentRepositoryProvisionerTests {

	private static final SkillVocabulary VOCABULARY = SkillVocabulary.fromClasspath("vocabulary/java-spring.json");

	private static final ArchetypeCatalog ARCHETYPES = ArchetypeCatalog.fromClasspath("archetypes/catalog.json");

	static final String TEMPLATE_README = """
			# Vaga Viva: modelo Java/Spring

			Repositório-modelo do arquétipo `java-spring`.

			## Tecnologias

			Java 21 e Spring Boot 4.
			""";

	private static final ProjectPlan PLAN = new ProjectPlanner(
			MissionCatalog.fromClasspath("missions/java-spring.json", VOCABULARY))
		.plan(List.of(SeniorityLevel.JUNIOR), Set.of("spring", "sql", "postgresql", "containers"));

	private final FakeGitHub github = new FakeGitHub();

	private final List<Duration> pauses = new ArrayList<>();

	private final StudentRepositoryProvisioner provisioner = provisionerWith(plan -> WrittenTexts.none());

	private StudentRepositoryProvisioner provisionerWith(MissionTextWriter writer) {
		return new StudentRepositoryProvisioner(github, Vocabularies.of(VOCABULARY), ARCHETYPES, writer, pauses::add);
	}

	@Test
	void createsTheRepositoryInTheStudentsAccount() {
		ProvisionedRepository repository = provisioner.provision("token", "maria", PLAN);

		assertThat(github.generated).containsExactly("maria/vaga-viva-clinic-scheduling");
		assertThat(repository.fullName()).isEqualTo("maria/vaga-viva-clinic-scheduling");
		assertThat(repository.htmlUrl()).isEqualTo("https://github.com/maria/vaga-viva-clinic-scheduling");
	}

	@Test
	void writesThePlanFileWithoutPersonalData() {
		provisioner.provision("token", "maria", PLAN);

		assertThat(github.files).filteredOn(file -> file.path().equals(".vaga-viva/plan.json"))
			.singleElement()
			.satisfies(file -> {
			assertThat(file.path()).isEqualTo(".vaga-viva/plan.json");
			assertThat(file.content()).isEqualToIgnoringWhitespace("""
					{ "archetype": "java-spring", "domain": "clinic-scheduling",
					  "missions": ["patients-api", "persistence", "pagination", "scheduling-rules",
					               "availability-query", "containerization"] }""");
			assertThat(file.content()).doesNotContain("maria");
		});
	}

	@Test
	void opensOneIssuePerMissionInPlanOrder() {
		ProvisionedRepository repository = provisioner.provision("token", "maria", PLAN);

		assertThat(github.issues).extracting(Issue::title)
			.containsExactly("Missão 1: Cadastro de pacientes", "Missão 2: Pacientes no banco de dados",
					"Missão 3: Listagem paginada de consultas", "Missão 4: Regras de agendamento",
					"Missão 5: Consulta de horários livres", "Missão 6: Aplicação em contêiner");
		assertThat(repository.missionIssues()).extracting(MissionIssue::missionId)
			.containsExactly("patients-api", "persistence", "pagination", "scheduling-rules", "availability-query",
					"containerization");
		assertThat(repository.missionIssues()).extracting(MissionIssue::number).containsExactly(1, 2, 3, 4, 5, 6);
	}

	@Test
	void issueBodyExplainsTheMissionAndHowToCheckIt() {
		provisioner.provision("token", "maria", PLAN);

		assertThat(github.issues.getFirst().body())
			.contains("Criar a API REST para cadastrar, buscar, alterar e remover pacientes.")
			.contains("Programação em Java, Spring Framework, Design de APIs")
			.contains("./mvnw test -Dgroups=patients-api");
	}

	@Test
	void triesAnotherNameWhenTheRepositoryNameIsTaken() {
		github.failGenerate.add(422);

		ProvisionedRepository repository = provisioner.provision("token", "maria", PLAN);

		assertThat(github.generated).containsExactly("maria/vaga-viva-clinic-scheduling",
				"maria/vaga-viva-clinic-scheduling-2");
		assertThat(repository.fullName()).isEqualTo("maria/vaga-viva-clinic-scheduling-2");
	}

	@Test
	void givesUpAfterFiveTakenNames() {
		for (int i = 0; i < 5; i++) {
			github.failGenerate.add(422);
		}

		assertThatThrownBy(() -> provisioner.provision("token", "maria", PLAN))
			.isInstanceOfSatisfying(GitHubApiException.class, e -> assertThat(e.status()).isEqualTo(422));
		assertThat(github.generated).hasSize(5);
	}

	@Test
	void waitsForTheNewRepositoryWhileGitHubIsStillCreatingIt() {
		github.failCreateFile.add(404);
		github.failCreateFile.add(409);

		provisioner.provision("token", "maria", PLAN);

		assertThat(github.files).hasSize(1);
		assertThat(pauses).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(2));
	}

	@Test
	void waitsForTheTemplateCopyBeforeWritingThePlan() {
		github.emptyRepositoryChecks = 2;

		provisioner.provision("token", "maria", PLAN);

		assertThat(github.calls).startsWith("generate", "latestCommit", "latestCommit", "latestCommit", "createFile");
		assertThat(pauses).containsExactly(Duration.ofSeconds(1), Duration.ofSeconds(2));
		assertThat(github.files).hasSize(1);
	}

	@Test
	void givesUpWhenTheRepositoryNeverBecomesReady() {
		for (int i = 0; i < 5; i++) {
			github.failCreateFile.add(404);
		}

		assertThatThrownBy(() -> provisioner.provision("token", "maria", PLAN))
			.isInstanceOfSatisfying(GitHubApiException.class, e -> assertThat(e.status()).isEqualTo(404));
		assertThat(github.issues).isEmpty();
	}

	@Test
	void doesNotRetryOtherErrors() {
		github.failCreateFile.add(403);

		assertThatThrownBy(() -> provisioner.provision("token", "maria", PLAN))
			.isInstanceOfSatisfying(GitHubApiException.class, e -> assertThat(e.status()).isEqualTo(403));
		assertThat(pauses).isEmpty();
	}

	@Test
	void writtenTextsGoIntoIssuesAndReadme() {
		var contexts = new LinkedHashMap<String, String>();
		PLAN.missions().forEach(m -> contexts.put(m.mission().id(), "Contexto de " + m.mission().id() + "."));

		provisionerWith(plan -> new WrittenTexts(Optional.of("Intro do LLM."), contexts, Optional.empty()))
			.provision("token", "maria", PLAN);

		assertThat(github.issues).hasSize(6);
		for (int i = 0; i < 6; i++) {
			String id = PLAN.missions().get(i).mission().id();
			assertThat(github.issues.get(i).body())
				.startsWith("### Por que esta missão importa para a vaga\n\nContexto de " + id + ".");
		}
		assertThat(github.updates).singleElement().satisfies(file -> {
			assertThat(file.path()).isEqualTo("README.md");
			assertThat(file.content()).contains("## Seu projeto")
				.contains("Intro do LLM.")
				.contains("[#1](https://github.com/maria/vaga-viva-clinic-scheduling/issues/1)")
				.contains("## Tecnologias")
				.contains("Java 21 e Spring Boot 4.");
		});
	}

	@Test
	void defaultTextsWhenTheWriterGivesNothing() {
		provisioner.provision("token", "maria", PLAN);

		assertThat(github.issues.getFirst().body()).startsWith("### Por que esta missão importa para a vaga")
			.contains("Esta missão exercita");
		assertThat(github.updates).singleElement()
			.satisfies(file -> assertThat(file.content()).contains(DefaultStudentTexts.projectIntro(PLAN, VOCABULARY)));
	}

	@Test
	void defaultTextOnlyForMissionsTheWriterMissed() {
		provisionerWith(plan -> new WrittenTexts(Optional.empty(), Map.of("patients-api", "Texto do LLM."),
				Optional.empty()))
			.provision("token", "maria", PLAN);

		assertThat(github.issues.getFirst().body()).contains("Texto do LLM.");
		assertThat(github.issues.get(1).body()).doesNotContain("Texto do LLM.").contains("Esta missão exercita");
	}

	@Test
	void provisioningSurvivesAWriterThatThrows() {
		ProvisionedRepository repository = provisionerWith(plan -> {
			throw new IllegalStateException("boom");
		}).provision("token", "maria", PLAN);

		assertThat(repository.missionIssues()).hasSize(6);
		assertThat(github.issues.getFirst().body()).contains("Esta missão exercita");
		assertThat(github.updates).hasSize(1);
	}

	@Test
	void createsTheReadmeWhenTheTemplateHasNone() {
		github.readme = null;

		provisioner.provision("token", "maria", PLAN);

		assertThat(github.updates).isEmpty();
		assertThat(github.files).filteredOn(file -> file.path().equals("README.md"))
			.singleElement()
			.satisfies(file -> assertThat(file.content()).startsWith("# Vaga Viva: Agendamento de clínica")
				.contains("## Seu projeto"));
	}

	@Test
	void waitsWhenTheReadmeIsNotReadableYet() {
		github.failReadFile.add(409);

		provisioner.provision("token", "maria", PLAN);

		assertThat(pauses).containsExactly(Duration.ofSeconds(1));
		assertThat(github.updates).hasSize(1);
	}

	@Test
	void readmeFailureDoesNotFailProvisioning() {
		github.failUpdateFile.add(403);

		ProvisionedRepository repository = provisioner.provision("token", "maria", PLAN);

		assertThat(repository.missionIssues()).hasSize(PLAN.missions().size());
		assertThat(github.issues).hasSize(6);
		assertThat(github.updates).isEmpty();
	}

	record File(String repository, String path, String content) {
	}

	record Issue(String repository, String title, String body) {
	}

	/** In-memory GitHub that can fail the next calls with given statuses. */
	static class FakeGitHub implements GitHubApi {

		final List<String> generated = new ArrayList<>();

		final List<File> files = new ArrayList<>();

		final List<Issue> issues = new ArrayList<>();

		final Deque<Integer> failGenerate = new ArrayDeque<>();

		final Deque<Integer> failCreateFile = new ArrayDeque<>();

		final List<File> updates = new ArrayList<>();

		final Deque<Integer> failReadFile = new ArrayDeque<>();

		final Deque<Integer> failUpdateFile = new ArrayDeque<>();

		/** The README of the generated repository; {@code null} means the file does not exist. */
		String readme = TEMPLATE_README;

		final List<String> calls = new ArrayList<>();

		final List<String> templates = new ArrayList<>();

		/** How many times the new repository still looks empty, as while GitHub copies the template. */
		int emptyRepositoryChecks;

		@Override
		public GitHubRepository generateFromTemplate(String token, String templateRepository, String owner, String name,
				String description) {
			calls.add("generate");
			templates.add(templateRepository);
			generated.add(owner + "/" + name);
			failIfQueued(failGenerate);
			return new GitHubRepository(name, owner + "/" + name, "https://github.com/" + owner + "/" + name,
					new GitHubRepository.Owner(owner));
		}

		@Override
		public void createFile(String token, String repositoryFullName, String path, String commitMessage,
				String content) {
			calls.add("createFile");
			failIfQueued(failCreateFile);
			files.add(new File(repositoryFullName, path, content));
		}

		@Override
		public Optional<GitHubFile> readFile(String token, String repositoryFullName, String path) {
			failIfQueued(failReadFile);
			return Optional.ofNullable(readme).map(r -> new GitHubFile(r, "readme-sha"));
		}

		@Override
		public void updateFile(String token, String repositoryFullName, String path, String commitMessage,
				String content, String sha) {
			failIfQueued(failUpdateFile);
			updates.add(new File(repositoryFullName, path, content));
		}

		@Override
		public String latestCommitSha(String token, String repositoryFullName) {
			calls.add("latestCommit");
			if (emptyRepositoryChecks > 0) {
				emptyRepositoryChecks--;
				throw new GitHubApiException(409, "Git Repository is empty.");
			}
			return "template-commit";
		}

		@Override
		public GitHubIssue createIssue(String token, String repositoryFullName, String title, String body) {
			issues.add(new Issue(repositoryFullName, title, body));
			int number = issues.size();
			return new GitHubIssue(number, "https://github.com/" + repositoryFullName + "/issues/" + number);
		}

		@Override
		public GitHubUser currentUser(String token) {
			return new GitHubUser("maria");
		}

		@Override
		public boolean isAppInstalledOn(String token, String login) {
			return true;
		}

		@Override
		public InstallationToken createInstallationToken(String appJwt, long installationId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public OptionalLong repositoryInstallation(String appJwt, String repositoryFullName) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Optional<GitHubPullRequest> latestPullRequest(String token, String repositoryFullName) {
			throw new UnsupportedOperationException();
		}

		@Override
		public GitHubPullRequest pullRequest(String token, String repositoryFullName, int number) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String pullRequestDiff(String token, String repositoryFullName, int number) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<GitHubComment> issueComments(String token, String repositoryFullName, int number) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<GitHubWorkflowRun> workflowRuns(String token, String repositoryFullName, String headSha) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<GitHubArtifact> runArtifacts(String token, String repositoryFullName, long runId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte[] downloadArtifact(String token, String repositoryFullName, long artifactId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public GitHubComment createIssueComment(String token, String repositoryFullName, int number, String body) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void updateIssueComment(String token, String repositoryFullName, long commentId, String body) {
			throw new UnsupportedOperationException();
		}

		private static void failIfQueued(Deque<Integer> failures) {
			Integer status = failures.poll();
			if (status != null) {
				throw new GitHubApiException(status, "simulated");
			}
		}

	}

}
