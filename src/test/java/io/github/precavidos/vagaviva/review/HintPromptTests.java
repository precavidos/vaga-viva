package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/** What the hint prompt tells the LLM, for each level. */
class HintPromptTests {

	private static final Mission PERSISTENCE = MissionCatalog
		.fromClasspath("missions/java-spring.json", SkillVocabulary.fromClasspath("vocabulary/java-spring.json"))
		.missions()
		.stream()
		.filter(m -> m.id().equals("persistence"))
		.findFirst()
		.orElseThrow();

	@Test
	void tellsTheLevelTheContractTheEarlierHintsAndTheDiff() {
		String user = HintPrompt.user(context(), PERSISTENCE, 2,
				List.of(new FailedTest("persistence", "gravaNoBanco", "esperava 201")), "# Contrato",
				List.of("Primeira dica"));

		assertThat(user).startsWith("Nível da dica: 2 de 3")
			.contains("Pacientes no banco de dados")
			.contains("# Contrato")
			.contains("gravaNoBanco: esperava 201")
			.contains("Primeira dica")
			.contains("diff --git a/X b/X")
			.doesNotContain("secret-token");
	}

	@Test
	void saysWhenNoTestOfTheMissionIsKnownToFail() {
		String user = HintPrompt.user(context(), PERSISTENCE, 1, List.of(), "# Contrato", List.of());

		assertThat(user).contains("Nenhum teste desta missão falha no relatório ou o relatório não está disponível.")
			.doesNotContain("Suas dicas anteriores");
	}

	@Test
	void systemPromptDescribesTheThreeLevels() {
		assertThat(HintPrompt.SYSTEM).contains("Nível 1")
			.contains("Nível 2")
			.contains("Nível 3")
			.contains("Nunca escreva código");
	}

	private static ReviewContext context() {
		return new ReviewContext("secret-token", "maria/repo", 7, "abc",
				new StudentPlan("java-spring", "clinic-scheduling", List.of(PERSISTENCE)), "diff --git a/X b/X", true,
				List.of(), List.of());
	}

}
