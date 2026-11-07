package io.github.precavidos.vagaviva.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubApiException;
import io.github.precavidos.vagaviva.github.GitHubIssue;
import io.github.precavidos.vagaviva.github.GitHubRepository;
import io.github.precavidos.vagaviva.github.GitHubUser;
import io.github.precavidos.vagaviva.jobposting.SeniorityLevel;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import io.github.precavidos.vagaviva.plan.ProjectPlanner;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

class StudentRepositoryProvisionerTests {

	private static final SkillVocabulary VOCABULARY = SkillVocabulary.fromClasspath("vocabulary/java-spring.json");

	private static final ProjectPlan PLAN = new ProjectPlanner(
			MissionCatalog.fromClasspath("missions/java-spring.json", VOCABULARY))
		.plan(List.of(SeniorityLevel.JUNIOR), Set.of("spring", "sql", "postgresql", "containers"));

	private final FakeGitHub github = new FakeGitHub();

	private final List<Duration> pauses = new ArrayList<>();

	private final StudentRepositoryProvisioner provisioner = new StudentRepositoryProvisioner(github, VOCABULARY,
			pauses::add);

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

		assertThat(github.files).singleElement().satisfies(file -> {
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

		@Override
		public GitHubRepository generateFromTemplate(String token, String owner, String name, String description) {
			generated.add(owner + "/" + name);
			failIfQueued(failGenerate);
			return new GitHubRepository(name, owner + "/" + name, "https://github.com/" + owner + "/" + name,
					new GitHubRepository.Owner(owner));
		}

		@Override
		public void createFile(String token, String repositoryFullName, String path, String commitMessage,
				String content) {
			failIfQueued(failCreateFile);
			files.add(new File(repositoryFullName, path, content));
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

		private static void failIfQueued(Deque<Integer> failures) {
			Integer status = failures.poll();
			if (status != null) {
				throw new GitHubApiException(status, "simulated");
			}
		}

	}

}
