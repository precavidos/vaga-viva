package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.github.precavidos.vagaviva.archetype.ArchetypeCatalog;
import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubComment;
import io.github.precavidos.vagaviva.github.GitHubFile;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.PreviousComment;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/** What the reviewer posts, and where, for each state of the pull request. */
class SocraticReviewerTests {

	private static final String REPO = "maria/vaga-viva-clinic-scheduling";

	private static final MissionCatalog CATALOG = MissionCatalog.fromClasspath("missions/java-spring.json",
			SkillVocabulary.fromClasspath("vocabulary/java-spring.json"));

	private static final StudentPlan PLAN = new StudentPlan("java-spring", "clinic-scheduling",
			CATALOG.missions().stream().filter(m -> List.of("patients-api", "persistence").contains(m.id())).toList());

	private static final Map<String, MissionTally> TALLIES = Map.of("patients-api", new MissionTally(5, 5),
			"persistence", new MissionTally(1, 4));

	private static final String CODE_DIFF = DiffChangesTests.diff("src/main/java/Patient.java", "+@Entity");

	private final GitHubApi github = mock(GitHubApi.class);

	private final List<Mission> focuses = new ArrayList<>();

	private final List<String> contracts = new ArrayList<>();

	private final List<String> asked = new ArrayList<>();

	private Optional<ReviewWriter.Written> llm = Optional.of(new ReviewWriter.Written(
			"Você já criou a entidade Patient e a tabela aparece no diff.",
			List.of("1. De onde o repositório lê os pacientes hoje?", "O que acontece com eles quando a aplicação reinicia?")));

	private SocraticReviewer reviewer;

	@BeforeEach
	void setUp() {
		reviewer = new SocraticReviewer(github, (context, focus, failing, contract) -> {
			focuses.add(focus);
			contracts.add(contract);
			return llm;
		}, new ReviewEvents() {

			@Override
			public boolean tracks(String repositoryFullName) {
				return true;
			}

			@Override
			public void activity(String repositoryFullName) {
			}

			@Override
			public void testsRan(String repositoryFullName, Integer pullRequestNumber, TestReport report) {
			}

			@Override
			public void reviewerAsked(String repositoryFullName, int pullRequestNumber, String question) {
				asked.add(repositoryFullName + "#" + pullRequestNumber + ": " + question);
			}

			@Override
			public void hintGiven(String repositoryFullName, String missionId) {
			}

		}, ArchetypeCatalog.fromClasspath("archetypes/catalog.json"));
		when(github.readFile("token", REPO, "missoes/persistence.md"))
			.thenReturn(Optional.of(new GitHubFile("# Contrato de persistence", "sha")));
		when(github.createIssueComment(anyString(), anyString(), anyInt(), anyString()))
			.thenReturn(new GitHubComment(1, "vaga-viva[bot]", true, "x"));
	}

	@Test
	void buildsTheSkeletonAroundTheLlmFields() {
		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco"), failed("persistence", "leDoBanco")),
				List.of()));

		assertThat(focuses).extracting(Mission::id).containsExactly("persistence");
		assertThat(contracts).containsExactly("# Contrato de persistence");
		String body = postedBody();
		assertThat(body).startsWith("<!-- vaga-viva:review focus=persistence sha=abc1234 -->\n## 🔎 Revisão do seu PR")
			.contains("**Missão em foco:** 2 · Pacientes no banco de dados · [contrato da missão](https://github.com/"
					+ REPO + "/blob/main/missoes/persistence.md)")
			.contains("### 📊 Seu progresso")
			.contains("| 1 · Cadastro de pacientes | 🟩🟩🟩🟩🟩 5/5 ✅ |")
			.contains("| **2 · Pacientes no banco de dados** | 🟩🟥🟥🟥 1/4 |")
			.contains("### ✅ O que já está funcionando\n\nVocê já criou a entidade Patient")
			.contains("| `gravaNoBanco` | esperava algo em gravaNoBanco |")
			.contains("### 🤔 Perguntas para pensar\n\n1. De onde o repositório lê os pacientes hoje?\n"
					+ "2. O que acontece com eles quando a aplicação reinicia?")
			.contains("1. Rode só os testes desta missão: `./mvnw test -Dgroups=persistence`")
			.contains("[issues do repositório](https://github.com/" + REPO + "/issues)")
			.endsWith(ReviewComment.FOOTER);
	}

	@Test
	void readsBackPlainTextForTheDashboardAndTheEmail() {
		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), List.of()));

		assertThat(ReviewTexts.reviewText(postedBody())).contains("Você já criou a entidade Patient e a tabela aparece no diff."
				+ "\n\n1. De onde o repositório lê os pacientes hoje?\n2. O que acontece com eles quando a aplicação reinicia?");
	}

	@Test
	void neverPraisesAPullRequestThatDoesNotChangeTheCode() {
		reviewer.review(context(DiffChangesTests.diff("README.md", "+comecei"), true,
				List.of(failed("persistence", "gravaNoBanco")), List.of()));

		assertThat(postedBody()).doesNotContain("Você já criou a entidade")
			.contains("Na missão **Pacientes no banco de dados**, 1 de 4 testes já passam.")
			.contains("1. De onde o repositório lê os pacientes hoje?");
	}

	@Test
	void fallsBackToTheFixedTextsWhenTheLlmHasNothing() {
		llm = Optional.empty();

		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), List.of()));

		assertThat(postedBody()).contains("Na missão **Pacientes no banco de dados**, 1 de 4 testes já passam.")
			.contains("1. O que o teste **gravaNoBanco** espera receber")
			.contains("missoes/persistence.md, diz sobre esse caso?")
			.contains("| `gravaNoBanco` | esperava algo em gravaNoBanco |");
	}

	@Test
	void dropsOnlyTheFieldTheFilterRefuses() {
		llm = Optional.of(new ReviewWriter.Written("Faça assim:\n```java\nrepository.save(patient);\n```",
				List.of("Onde os pacientes ficam guardados?", "Use repository.save(patient);")));

		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), List.of()));

		assertThat(postedBody()).doesNotContain("repository.save")
			.contains("1 de 4 testes já passam")
			.contains("### 🤔 Perguntas para pensar\n\n1. Onde os pacientes ficam guardados?\n\n### 👉 Próximo passo");
	}

	@Test
	void keepsAQuestionThatNamesAnAnnotationWithoutPingingAnyone() {
		llm = Optional.of(new ReviewWriter.Written("Bom começo.",
				List.of("O que muda quando a classe recebe @Entity?", "Onde o id é gerado?")));

		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), List.of()));

		assertThat(postedBody()).contains("1. O que muda quando a classe recebe @\u200BEntity?")
			.contains("2. Onde o id é gerado?")
			.doesNotContain("@Entity");
	}

	@Test
	void neverQuotesHostileTestNamesOrMessages() {
		llm = Optional.empty();

		reviewer.review(context(true, List.of(new FailedTest("persistence", "grava|`@maria`", "veio @maria e #3"),
				new FailedTest("persistence", "leDoBanco", "<!-- vaga-viva:review --> repository.save(patient);")),
				List.of()));

		String body = postedBody();
		assertThat(body).doesNotContain("@maria").doesNotContain(" #3").doesNotContain("repository.save")
			.contains("grava\\|")
			.contains("a mensagem não é repetida aqui");
		assertThat(body.indexOf("<!--", 1)).isEqualTo(body.indexOf("<!-- vaga-viva:summary"));
	}

	@Test
	void showsAtMostFiveFailingTests() {
		var failing = IntStream.rangeClosed(1, 7).mapToObj(i -> failed("persistence", "teste" + i)).toList();

		reviewer.review(context(true, failing, List.of()));

		assertThat(postedBody()).contains("`teste5`").doesNotContain("`teste6`").contains("… e mais 2 testes.");
	}

	@Test
	void celebratesTheMissionThePreviousReviewWasAbout() {
		var earlier = List.of(new PreviousComment(41, "<!-- vaga-viva:review focus=patients-api -->\n## Revisão"));

		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), earlier));

		assertThat(postedBody()).contains(
				"🎉 **Missão 1 concluída:** os 5 testes de **Cadastro de pacientes** passam. Agora é a vez da próxima.");
	}

	@Test
	void doesNotCelebrateWhileTheSameMissionIsInFocus() {
		var earlier = List.of(new PreviousComment(41, "<!-- vaga-viva:review focus=persistence -->\n## Revisão"));

		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), earlier));

		assertThat(postedBody()).doesNotContain("🎉");
	}

	@Test
	void usesTheMissionSummaryWhenTheContractFileIsMissing() {
		when(github.readFile("token", REPO, "missoes/patients-api.md")).thenReturn(Optional.empty());

		reviewer.review(context(true, List.of(failed("patients-api", "criaPaciente")), List.of()));

		assertThat(contracts).containsExactly(
				CATALOG.missions().stream().filter(m -> m.id().equals("patients-api")).findFirst().orElseThrow().summary());
	}

	@Test
	void listsTheStepsWhenTheTestsDidNotRun() {
		reviewer.review(context(false, List.of(), List.of()));

		assertThat(postedBody()).contains("### ⚠️ Os testes não rodaram")
			.contains("1. Abra a aba **Actions** deste PR.")
			.contains("Re-run jobs")
			.doesNotContain("Seu progresso");
		assertThat(focuses).isEmpty();
	}

	@Test
	void congratulatesWithTheProgressWhenEveryTestOfThePlanPasses() {
		reviewer.review(new ReviewContext("token", REPO, 7, "abc123", PLAN, "diff", true, List.of(), List.of(),
				Map.of("patients-api", new MissionTally(5, 5), "persistence", new MissionTally(4, 4))));

		assertThat(postedBody()).contains("🎉 **Todas as missões do seu plano passam neste PR.**")
			.contains("| 2 · Pacientes no banco de dados | 🟩🟩🟩🟩 4/4 ✅ |")
			.contains("2. Faça o merge deste PR.");
		assertThat(focuses).isEmpty();
	}

	@Test
	void postsANewReviewAndCollapsesTheLatestOneNotAHint() {
		var earlier = List.of(new PreviousComment(41, ReviewTexts.MARKER + "\nRevisão antiga"),
				new PreviousComment(42, HintTexts.marker("persistence", 1) + "\nUma dica " + ReviewTexts.MARKER));

		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), earlier));

		assertThat(postedBody()).startsWith("<!-- vaga-viva:review focus=persistence sha=abc1234 -->");
		assertThat(updatedBody(41)).isEqualTo(ReviewTexts.MARKER
				+ "\n<details>\n<summary>🔎 Revisão anterior (clique para abrir)</summary>\n\nRevisão antiga\n\n</details>");
		verify(github, never()).updateIssueComment(anyString(), anyString(), eq(42L), anyString());
	}

	@Test
	void namesTheCommitOfTheReviewItCollapses() {
		var earlier = List.of(new PreviousComment(41,
				"<!-- vaga-viva:review focus=persistence sha=9f8e7d6 -->\n## 🔎 Revisão do seu PR\n\nTexto"));

		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), earlier));

		assertThat(updatedBody(41)).contains("<summary>🔎 Revisão anterior · commit 9f8e7d6 (clique para abrir)</summary>")
			.doesNotContain("## 🔎 Revisão do seu PR")
			.contains("Texto");
	}

	@Test
	void leavesAnAlreadyCollapsedReviewAlone() {
		var earlier = List.of(new PreviousComment(41, ReviewTexts.MARKER + "\n<details>\n<summary>x</summary>\n</details>"));

		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), earlier));

		postedBody();
		verify(github, never()).updateIssueComment(anyString(), anyString(), anyLong(), anyString());
	}

	@Test
	void keepsTheNewReviewWhenCollapsingTheOldOneFails() {
		doThrow(new IllegalStateException("GitHub is down")).when(github)
			.updateIssueComment(anyString(), anyString(), anyLong(), anyString());

		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")),
				List.of(new PreviousComment(41, ReviewTexts.MARKER + "\nRevisão antiga"))));

		assertThat(postedBody()).contains("Revisão do seu PR");
		assertThat(asked).hasSize(1);
	}

	@Test
	void tellsTheEventsTheFirstQuestion() {
		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), List.of()));

		assertThat(asked).containsExactly(REPO + "#7: De onde o repositório lê os pacientes hoje?");
	}

	@Test
	void barsScaleDownAndLeaveOtherMissionsGrey() {
		assertThat(ReviewComment.bar(new MissionTally(1, 3), false)).isEqualTo("🟩⬜⬜ 1/3");
		assertThat(ReviewComment.bar(new MissionTally(10, 20), true)).isEqualTo("🟩🟩🟩🟩🟩🟥🟥🟥🟥🟥 10/20");
		assertThat(ReviewComment.bar(null, true)).isEqualTo("sem resultado ainda");
	}

	private String postedBody() {
		var body = ArgumentCaptor.forClass(String.class);
		verify(github).createIssueComment(eq("token"), eq(REPO), eq(7), body.capture());
		return body.getValue();
	}

	private String updatedBody(long id) {
		var body = ArgumentCaptor.forClass(String.class);
		verify(github).updateIssueComment(eq("token"), eq(REPO), eq(id), body.capture());
		return body.getValue();
	}

	private static ReviewContext context(boolean reportRead, List<FailedTest> failed, List<PreviousComment> comments) {
		return context(CODE_DIFF, reportRead, failed, comments);
	}

	private static ReviewContext context(String diff, boolean reportRead, List<FailedTest> failed,
			List<PreviousComment> comments) {
		return new ReviewContext("token", REPO, 7, "abc1234def", PLAN, diff, reportRead, failed, comments,
				reportRead ? TALLIES : Map.of());
	}

	private static FailedTest failed(String mission, String test) {
		return new FailedTest(mission, test, "esperava algo em " + test);
	}

}
