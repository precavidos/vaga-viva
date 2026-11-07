package io.github.precavidos.vagaviva.writing;

import static io.github.precavidos.vagaviva.jobposting.SeniorityLevel.JUNIOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import io.github.precavidos.vagaviva.plan.ProjectPlanner;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

class MissionTextPromptTests {

	private static final SkillVocabulary VOCABULARY = SkillVocabulary.fromClasspath("vocabulary/java-spring.json");

	private static final ProjectPlanner PLANNER = new ProjectPlanner(
			MissionCatalog.fromClasspath("missions/java-spring.json", VOCABULARY));

	@Test
	void listsThePlanInOrderWithLabels() {
		ProjectPlan plan = PLANNER.plan(List.of(JUNIOR), List.of("spring", "sql"), List.of("spring-boot", "postgresql"));

		String user = MissionTextPrompt.user(plan, VOCABULARY);

		assertThat(user).contains("Nível da vaga: júnior")
			.contains("Competências pedidas na vaga: Spring Framework; SQL")
			.contains("Tecnologias citadas na vaga: Spring Boot; PostgreSQL");
		assertThat(user.indexOf("1. id: patients-api")).isPositive()
			.isLessThan(user.indexOf("2. id: persistence"));
	}

	@Test
	void formatsTheWholeUserMessage() {
		ProjectPlan plan = PLANNER.plan(List.of(JUNIOR), List.of("spring", "sql"), List.of("spring-boot", "postgresql"));

		assertThat(MissionTextPrompt.user(plan, VOCABULARY)).startsWith("""
				Arquétipo: java-spring
				Domínio fictício: Agendamento de clínica. Sistema de uma clínica para cadastrar pacientes e agendar consultas com profissionais de saúde.
				Nível da vaga: júnior
				Competências pedidas na vaga: Spring Framework; SQL
				Tecnologias citadas na vaga: Spring Boot; PostgreSQL

				Missões, na ordem do plano:
				1. id: patients-api
				   título: Cadastro de pacientes
				   resumo: Criar a API REST para cadastrar, buscar, alterar e remover pacientes.
				   competências da vaga que exercita: Spring Framework
				2. id: persistence
				""");
	}

	@Test
	void saysNoneWhenThePostingHasNoSkills() {
		ProjectPlan plan = PLANNER.plan(List.of(JUNIOR), Set.of());

		assertThat(MissionTextPrompt.user(plan, VOCABULARY)).contains("Competências pedidas na vaga: (nenhuma)");
	}

	@Test
	void fallsBackToTheIdWhenAnItemHasNoLabel() {
		ProjectPlan plan = PLANNER.plan(List.of(JUNIOR), List.of("unknown-skill"));

		assertThat(MissionTextPrompt.user(plan, VOCABULARY)).contains("Competências pedidas na vaga: unknown-skill");
	}

	@Test
	void systemPromptForbidsChangingThePlan() {
		assertThat(MissionTextPrompt.SYSTEM).contains("você não muda, não reordena e não sugere missões")
			.contains("repita exatamente o mesmo id");
	}

}
