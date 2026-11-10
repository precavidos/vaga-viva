package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

	private final GitHubApi github = mock(GitHubApi.class);

	private final List<Mission> focuses = new ArrayList<>();

	private final List<String> contracts = new ArrayList<>();

	private Optional<String> llmText = Optional.of("Muito bom ter criado o controller. O que o teste espera receber quando o paciente é criado?");

	private SocraticReviewer reviewer;

	@BeforeEach
	void setUp() {
		reviewer = new SocraticReviewer(github, (context, focus, failing, contract) -> {
			focuses.add(focus);
			contracts.add(contract);
			return llmText;
		});
		when(github.readFile("token", REPO, "missoes/persistence.md"))
			.thenReturn(Optional.of(new GitHubFile("# Contrato de persistence", "sha")));
		when(github.createIssueComment(anyString(), anyString(), anyInt(), anyString()))
			.thenReturn(new GitHubComment(1, "vaga-viva[bot]", true, "x"));
	}

	@Test
	void asksAboutTheFirstFailingMissionOfThePlanWithTheLlmText() {
		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco"), failed("persistence", "leDoBanco")),
				List.of()));

		assertThat(focuses).extracting(Mission::id).containsExactly("persistence");
		assertThat(contracts).containsExactly("# Contrato de persistence");
		assertThat(postedBody()).startsWith(ReviewTexts.MARKER)
			.contains("O que o teste espera receber")
			.endsWith(ReviewTexts.FOOTER);
	}

	@Test
	void fallsBackToTheFixedQuestionWhenTheLlmHasNothing() {
		llmText = Optional.empty();

		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), List.of()));

		assertThat(postedBody()).contains("Pacientes no banco de dados")
			.contains("1 teste ainda falha")
			.contains("> esperava algo em gravaNoBanco")
			.contains("missoes/persistence.md");
	}

	@Test
	void refusesAnLlmTextThatContainsCode() {
		llmText = Optional.of("Faça assim:\n```java\nrepository.save(patient);\n```");

		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), List.of()));

		assertThat(postedBody()).doesNotContain("repository.save").contains("1 teste ainda falha");
	}

	@Test
	void usesTheMissionSummaryWhenTheContractFileIsMissing() {
		when(github.readFile("token", REPO, "missoes/patients-api.md")).thenReturn(Optional.empty());

		reviewer.review(context(true, List.of(failed("patients-api", "criaPaciente")), List.of()));

		assertThat(contracts).containsExactly(
				CATALOG.missions().stream().filter(m -> m.id().equals("patients-api")).findFirst().orElseThrow().summary());
	}

	@Test
	void saysTheBuildDidNotRunTheTestsWhenThereIsNoReport() {
		reviewer.review(context(false, List.of(), List.of()));

		assertThat(postedBody()).contains("terminou sem gerar o relatório de testes").contains("não compila");
		assertThat(focuses).isEmpty();
	}

	@Test
	void congratulatesWhenEveryTestOfThePlanPasses() {
		reviewer.review(context(true, List.of(), List.of()));

		assertThat(postedBody()).contains("Todos os testes das missões do seu plano passam")
			.contains("Cadastro de pacientes, Pacientes no banco de dados");
		assertThat(focuses).isEmpty();
	}

	@Test
	void editsItsOwnEarlierReviewInsteadOfPostingAnother() {
		var earlier = List.of(new PreviousComment(40, "Uma dica de nível 1 <!-- vaga-viva:hint -->"),
				new PreviousComment(41, ReviewTexts.MARKER + "\nRevisão antiga"));

		reviewer.review(context(true, List.of(failed("persistence", "gravaNoBanco")), earlier));

		var body = ArgumentCaptor.forClass(String.class);
		verify(github).updateIssueComment(eq("token"), eq(REPO), eq(41L), body.capture());
		assertThat(body.getValue()).contains("O que o teste espera receber");
		verify(github, never()).createIssueComment(anyString(), anyString(), anyInt(), anyString());
	}

	private String postedBody() {
		var body = ArgumentCaptor.forClass(String.class);
		verify(github).createIssueComment(eq("token"), eq(REPO), eq(7), body.capture());
		verify(github, never()).updateIssueComment(anyString(), anyString(), anyLong(), anyString());
		return body.getValue();
	}

	private static ReviewContext context(boolean reportRead, List<FailedTest> failed, List<PreviousComment> comments) {
		return new ReviewContext("token", REPO, 7, "abc123", PLAN, "diff --git a/x b/x", reportRead, failed, comments);
	}

	private static FailedTest failed(String mission, String test) {
		return new FailedTest(mission, test, "esperava algo em " + test);
	}

}
