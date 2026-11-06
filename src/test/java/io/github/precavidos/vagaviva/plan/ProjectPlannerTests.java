package io.github.precavidos.vagaviva.plan;

import static io.github.precavidos.vagaviva.jobposting.SeniorityLevel.INTERN;
import static io.github.precavidos.vagaviva.jobposting.SeniorityLevel.JUNIOR;
import static io.github.precavidos.vagaviva.jobposting.SeniorityLevel.MID;
import static io.github.precavidos.vagaviva.jobposting.SeniorityLevel.SENIOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.archetype.ArchetypeCatalog;
import io.github.precavidos.vagaviva.jobposting.ExtractedJobPosting;
import io.github.precavidos.vagaviva.jobposting.SeniorityLevel;
import io.github.precavidos.vagaviva.plan.PlannedMission.Reason;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

class ProjectPlannerTests {

	private static final SkillVocabulary VOCABULARY = SkillVocabulary.fromClasspath("vocabulary/java-spring.json");

	private static final ArchetypeCatalog ARCHETYPES = ArchetypeCatalog.fromClasspath("archetypes/catalog.json");

	private static final ProjectPlanner PLANNER = new ProjectPlanner(
			MissionCatalog.fromClasspath("missions/java-spring.json", VOCABULARY));

	@Test
	void postingWithNoSkillsGetsCoreMissionsPlusDefaultsInDifficultyOrder() {
		var plan = PLANNER.plan(List.of(), Set.of());

		assertThat(ids(plan)).containsExactly("patients-api", "persistence", "input-validation", "bug-hunt",
				"scheduling-rules");
		assertThat(plan.missions()).extracting(PlannedMission::reason)
			.containsExactly(Reason.CORE, Reason.CORE, Reason.DEFAULT, Reason.DEFAULT, Reason.CORE);
		assertThat(plan.domain().id()).isEqualTo("clinic-scheduling");
	}

	@Test
	void noRecognizedLevelIsPlannedAsJunior() {
		var plan = PLANNER.plan(List.of(), Set.of());

		assertThat(plan.seniorityLevel()).isEqualTo(JUNIOR);
		assertThat(plan.difficultyCap()).isEqualTo(3);
	}

	@Test
	void juniorPlansLeaveOutMissionsAboveTheCap() {
		var plan = PLANNER.plan(List.of(JUNIOR), Set.of("web-security", "spring", "containers", "sql", "postgresql"));

		assertThat(ids(plan)).doesNotContain("api-security")
			.containsExactly("patients-api", "persistence", "pagination", "scheduling-rules", "availability-query",
					"containerization");
	}

	@Test
	void midLevelPlansCanIncludeTheHardestMissions() {
		var plan = PLANNER.plan(List.of(MID), Set.of("web-security", "spring"));

		assertThat(plan.difficultyCap()).isEqualTo(4);
		assertThat(ids(plan)).containsExactly("patients-api", "persistence", "pagination", "scheduling-rules",
				"api-security");
	}

	@Test
	void seniorPlansUseTheSameCapAsMid() {
		assertThat(PLANNER.plan(List.of(SENIOR), Set.of()).difficultyCap()).isEqualTo(4);
	}

	@Test
	void severalLevelsUseTheLowest() {
		var plan = PLANNER.plan(List.of(JUNIOR, MID), Set.of("web-security"));

		assertThat(plan.seniorityLevel()).isEqualTo(JUNIOR);
		assertThat(ids(plan)).doesNotContain("api-security");
	}

	@Test
	void internPlansUseTheJuniorCap() {
		assertThat(PLANNER.plan(List.of(INTERN), Set.of()).difficultyCap()).isEqualTo(3);
	}

	@Test
	void fillsWithDefaultsWhenTooFewSkillsMatch() {
		var plan = PLANNER.plan(List.of(JUNIOR), Set.of("containers"));

		assertThat(plan.missions()).hasSize(5);
		assertThat(plan.missions()).filteredOn(m -> m.reason() == Reason.DEFAULT)
			.extracting(m -> m.mission().id())
			.containsExactly("input-validation");
	}

	@Test
	void picksAtMostThreeSkillMatchesPreferringTheBestMatches() {
		var plan = PLANNER.plan(List.of(MID), Set.of("sql", "postgresql", "data-modeling", "web-security", "spring",
				"design-patterns", "object-oriented-programming", "continuous-integration", "containers"));

		assertThat(plan.missions()).hasSize(6);
		assertThat(plan.missions()).filteredOn(m -> m.reason() == Reason.SKILL_MATCH)
			.extracting(m -> m.mission().id())
			.containsExactlyInAnyOrder("availability-query", "reminder-strategies", "api-security");
	}

	@Test
	void keepsTheMatchedSkillsInMissionOrder() {
		var plan = PLANNER.plan(List.of(JUNIOR), Set.of("postgresql", "sql"));

		assertThat(plan.missions()).filteredOn(m -> m.mission().id().equals("availability-query"))
			.singleElement()
			.satisfies(m -> {
				assertThat(m.reason()).isEqualTo(Reason.SKILL_MATCH);
				assertThat(m.matchedSkills()).containsExactly("sql", "postgresql");
			});
	}

	@Test
	void coreMissionsAlsoListTheSkillsTheyMatch() {
		var plan = PLANNER.plan(List.of(JUNIOR), Set.of("java", "spring"));

		assertThat(plan.missions()).filteredOn(m -> m.mission().id().equals("patients-api"))
			.singleElement()
			.satisfies(m -> {
				assertThat(m.reason()).isEqualTo(Reason.CORE);
				assertThat(m.matchedSkills()).containsExactly("java", "spring");
			});
	}

	@Test
	void samePostingAlwaysGetsTheSamePlan() {
		var skills = Set.of("spring", "sql", "postgresql", "containers", "debugging");

		assertThat(PLANNER.plan(List.of(JUNIOR), skills)).isEqualTo(PLANNER.plan(List.of(JUNIOR), skills));
	}

	@Test
	void plansFromARealPostingText() {
		var recognition = VOCABULARY.recognize("Desenvolvedor Java Júnior com Spring Boot, PostgreSQL e Docker");
		var skills = recognition.skills().stream().map(s -> s.skill().id()).toList();

		var plan = PLANNER.plan(List.of(JUNIOR), skills);

		assertThat(ids(plan)).containsExactly("patients-api", "persistence", "pagination", "scheduling-rules",
				"availability-query", "containerization");
	}

	@Test
	void keepsThePostingsSkillsAndTechnologiesWithoutItsWords() {
		var text = "Dev Java Júnior com Spring Boot, PostgreSQL e Docker";
		var posting = new ExtractedJobPosting("java-spring", ARCHETYPES.match(text), List.of(JUNIOR),
				VOCABULARY.recognize(text));

		var plan = PLANNER.plan(posting);

		assertThat(plan.postingSkills()).containsExactlyElementsOf(
				posting.recognition().skills().stream().map(s -> s.skill().id()).toList());
		assertThat(plan.postingTechnologies()).contains("java", "spring-boot", "postgresql", "docker");
	}

	@Test
	void levelsHavePortugueseLabels() {
		assertThat(Arrays.stream(SeniorityLevel.values()).map(SeniorityLevel::label))
			.containsExactly("estágio", "júnior", "pleno", "sênior");
	}

	@Test
	void unorderedSkillsGiveADeterministicOrder() {
		var plan = PLANNER.plan(List.of(JUNIOR), Set.of("sql", "java", "spring"));

		assertThat(plan.postingSkills()).isEqualTo(List.of("java", "spring", "sql"));
	}

	private static List<String> ids(ProjectPlan plan) {
		return plan.missions().stream().map(m -> m.mission().id()).toList();
	}

}
