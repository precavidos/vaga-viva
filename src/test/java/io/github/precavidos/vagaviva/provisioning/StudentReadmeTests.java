package io.github.precavidos.vagaviva.provisioning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.jobposting.SeniorityLevel;
import io.github.precavidos.vagaviva.plan.FictionalDomain;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.plan.PlannedMission;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import io.github.precavidos.vagaviva.plan.ProjectPlanner;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

class StudentReadmeTests {

	private static final SkillVocabulary VOCABULARY = SkillVocabulary.fromClasspath("vocabulary/java-spring.json");

	private static final ProjectPlan PLAN = new ProjectPlanner(
			MissionCatalog.fromClasspath("missions/java-spring.json", VOCABULARY))
		.plan(List.of(SeniorityLevel.JUNIOR), Set.of("spring", "sql", "postgresql", "containers"));

	private static final String INTRO = "Você vai construir a API da clínica.";

	private static final String SECTION = "## Seu projeto\n\nVocê vai construir a API da clínica.\n";

	@Test
	void insertsTheSectionBeforeTheFirstHeading() {
		String section = StudentReadme.projectSection(INTRO, PLAN.missions(), issues());

		String readme = StudentReadme.withProjectSection(StudentRepositoryProvisionerTests.TEMPLATE_README, section);

		assertThat(readme).isEqualTo("""
				# Vaga Viva: modelo Java/Spring

				Repositório-modelo do arquétipo `java-spring`.

				""" + section + """

				## Tecnologias

				Java 21 e Spring Boot 4.
				""");
	}

	@Test
	void ignoresHeadingsInsideTildeFences() {
		String readme = "# Título\n\n~~~md\n## não é título\n~~~\n\n## Comandos\n\nRode o projeto.\n";

		String result = StudentReadme.withProjectSection(readme, SECTION);

		assertThat(result).isEqualTo("# Título\n\n~~~md\n## não é título\n~~~\n\n" + SECTION
				+ "\n## Comandos\n\nRode o projeto.\n");
	}

	@Test
	void ignoresHeadingsInsideIndentedFences() {
		String readme = "# Título\n\n  ```md\n## não é título\n  ```\n\n## Comandos\n";

		String result = StudentReadme.withProjectSection(readme, SECTION);

		assertThat(result).isEqualTo("# Título\n\n  ```md\n## não é título\n  ```\n\n" + SECTION + "\n## Comandos\n");
	}

	@Test
	void aBacktickLineDoesNotCloseATildeFence() {
		String readme = "# Título\n\n~~~md\n```\n## não é título\n~~~\n\n## Comandos\n";

		String result = StudentReadme.withProjectSection(readme, SECTION);

		assertThat(result).isEqualTo("# Título\n\n~~~md\n```\n## não é título\n~~~\n\n" + SECTION + "\n## Comandos\n");
	}

	@Test
	void ignoresHeadingsInsideCodeBlocks() {
		String readme = """
				# Título

				```md
				## não é título
				```

				## Comandos

				Rode o projeto.
				""";

		String result = StudentReadme.withProjectSection(readme, SECTION);

		assertThat(result).isEqualTo("""
				# Título

				```md
				## não é título
				```

				""" + SECTION + """

				## Comandos

				Rode o projeto.
				""");
	}

	@Test
	void doesNotTreatSmallerHeadingsAsSections() {
		String readme = "# Título\n\n### Detalhe\n\nTexto.\n\n## Tecnologias\n\nJava 21.\n";

		String result = StudentReadme.withProjectSection(readme, SECTION);

		assertThat(result).isEqualTo("# Título\n\n### Detalhe\n\nTexto.\n\n" + SECTION + "\n## Tecnologias\n\nJava 21.\n");
	}

	@Test
	void appendsWhenThereIsNoHeading() {
		assertThat(StudentReadme.withProjectSection("# Título\n\nSem seções.", SECTION))
			.isEqualTo("# Título\n\nSem seções.\n\n" + SECTION);
		assertThat(StudentReadme.withProjectSection("# Título\n\nSem seções.\n", SECTION))
			.isEqualTo("# Título\n\nSem seções.\n\n" + SECTION);
	}

	@Test
	void keepsWindowsLineEndings() {
		String readme = "# Título\r\n\r\n## Tecnologias\r\n\r\nJava 21.\r\n";

		String result = StudentReadme.withProjectSection(readme, SECTION);

		assertThat(result).isEqualTo("# Título\r\n\r\n" + SECTION + "\n## Tecnologias\r\n\r\nJava 21.\r\n");
	}

	@Test
	void sectionListsMissionsInPlanOrderWithIssueLinks() {
		List<MissionIssue> reversed = new ArrayList<>(issues());
		Collections.reverse(reversed);

		String section = StudentReadme.projectSection(INTRO, PLAN.missions(), reversed);

		StringBuilder expected = new StringBuilder("## Seu projeto\n\n" + INTRO + "\n\n");
		expected.append("| Ordem | Missão | Issue |\n| --- | --- | --- |\n");
		List<PlannedMission> missions = PLAN.missions();
		for (int order = 1; order <= missions.size(); order++) {
			expected.append("| %d | %s | [#%d](%s) |\n".formatted(order, missions.get(order - 1).mission().title(),
					order, issueUrl(order)));
		}
		assertThat(section).isEqualTo(expected.toString());
	}

	@Test
	void newReadmeNamesTheDomainAndStartsWithTheSection() {
		FictionalDomain domain = new FictionalDomain("clinic", "Clínica Horizonte", "Uma clínica fictícia.");

		assertThat(StudentReadme.newReadme(domain, SECTION))
			.isEqualTo("# Vaga Viva: Clínica Horizonte\n\n" + SECTION);
	}

	private static List<MissionIssue> issues() {
		return IntStream.rangeClosed(1, PLAN.missions().size())
			.mapToObj(n -> new MissionIssue(PLAN.missions().get(n - 1).mission().id(), n, issueUrl(n)))
			.toList();
	}

	private static String issueUrl(int number) {
		return "https://github.com/maria/vaga-viva-clinic-scheduling/issues/" + number;
	}

}
