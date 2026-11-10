package io.github.precavidos.vagaviva.provisioning;

import static io.github.precavidos.vagaviva.jobposting.SeniorityLevel.JUNIOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.archetype.ArchetypeFit;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.plan.PlannedMission;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import io.github.precavidos.vagaviva.plan.ProjectPlanner;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

class DefaultStudentTextsTests {

	private static final SkillVocabulary VOCABULARY = SkillVocabulary.fromClasspath("vocabulary/java-spring.json");

	private static final ProjectPlanner PLANNER = new ProjectPlanner(
			MissionCatalog.fromClasspath("missions/java-spring.json", VOCABULARY));

	@Test
	void joinsInPortuguese() {
		assertThat(DefaultStudentTexts.joinPt(List.of("A"))).isEqualTo("A");
		assertThat(DefaultStudentTexts.joinPt(List.of("A", "B"))).isEqualTo("A e B");
		assertThat(DefaultStudentTexts.joinPt(List.of("A", "B", "C"))).isEqualTo("A, B e C");
	}

	@Test
	void missionContextNamesTheSkillsThePostingAsksFor() {
		ProjectPlan plan = PLANNER.plan(List.of(JUNIOR), List.of("java", "spring"));
		PlannedMission mission = plan.missions()
			.stream()
			.filter(m -> m.mission().id().equals("patients-api"))
			.findFirst()
			.orElseThrow();

		assertThat(DefaultStudentTexts.missionContext(mission, VOCABULARY))
			.isEqualTo("Esta missão exercita Programação em Java e Spring Framework, pedidas na vaga.");
	}

	@Test
	void missionContextForAMissionWithNoMatchedSkills() {
		ProjectPlan plan = PLANNER.plan(List.of(JUNIOR), Set.of());
		PlannedMission mission = plan.missions().getFirst();

		assertThat(mission.matchedSkills()).isEmpty();
		assertThat(DefaultStudentTexts.missionContext(mission, VOCABULARY))
			.isEqualTo("Esta missão faz parte da base de todo projeto do Vaga Viva.");
	}

	@Test
	void projectIntroMentionsLevelSkillsAndDomain() {
		ProjectPlan plan = PLANNER.plan(List.of(JUNIOR), List.of("java", "spring"));

		assertThat(DefaultStudentTexts.projectIntro(plan, VOCABULARY)).isEqualTo("Este projeto foi montado para uma vaga de nível júnior que pede Programação em Java e Spring Framework. Ele se passa num domínio fictício, **Agendamento de clínica**: sistema de uma clínica para cadastrar pacientes e agendar consultas com profissionais de saúde. Cada missão abaixo tem uma issue com o que fazer e testes que só passam quando ela estiver pronta.");
	}

	@Test
	void projectIntroWithoutSkillsLeavesTheLevelOnly() {
		ProjectPlan plan = PLANNER.plan(List.of(JUNIOR), Set.of());

		assertThat(DefaultStudentTexts.projectIntro(plan, VOCABULARY)).startsWith("Este projeto foi montado para uma vaga de nível júnior. Ele se passa num domínio fictício, **Agendamento de clínica**: sistema de uma clínica")
			.endsWith("Cada missão abaixo tem uma issue com o que fazer e testes que só passam quando ela estiver pronta.");
	}

	@Test
	void projectIntroWarnsWhenTheStackIsPartlyCovered() {
		var fit = new ArchetypeFit("java-spring", ArchetypeFit.Status.PARTIAL, List.of("Java"), List.of("Quarkus"));
		ProjectPlan plan = PLANNER.plan(List.of(JUNIOR), List.of("java"), List.of("java"), fit);

		assertThat(DefaultStudentTexts.projectIntro(plan, VOCABULARY)).isEqualTo("Este projeto foi montado para uma vaga de nível júnior que pede Programação em Java. A vaga também pede Quarkus, que este projeto não pratica; o que ele treina em Java continua valendo para ela. Ele se passa num domínio fictício, **Agendamento de clínica**: sistema de uma clínica para cadastrar pacientes e agendar consultas com profissionais de saúde. Cada missão abaixo tem uma issue com o que fazer e testes que só passam quando ela estiver pronta.");
	}

	@Test
	void projectIntroWarnsWhenTheStackIsNotCovered() {
		var fit = new ArchetypeFit("java-spring", ArchetypeFit.Status.NOT_COVERED, List.of(),
				List.of("Python", "Django"));
		ProjectPlan plan = PLANNER.plan(List.of(JUNIOR), List.of(), List.of(), fit);

		assertThat(DefaultStudentTexts.projectIntro(plan, VOCABULARY)).isEqualTo("Este projeto foi montado para uma vaga de nível júnior. A vaga pede Python e Django, que este projeto não pratica; as competências que ele treina continuam valendo para a vaga, mas em outra stack. Ele se passa num domínio fictício, **Agendamento de clínica**: sistema de uma clínica para cadastrar pacientes e agendar consultas com profissionais de saúde. Cada missão abaixo tem uma issue com o que fazer e testes que só passam quando ela estiver pronta.");
	}

}
