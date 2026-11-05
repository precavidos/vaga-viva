package io.github.precavidos.vagaviva.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

class MissionCatalogTests {

	private static final SkillVocabulary VOCABULARY = SkillVocabulary.fromClasspath("vocabulary/java-spring.json");

	private static final String CORE = """
			{ "id": "a", "title": "A", "summary": "A", "difficulty": 1, "core": true, "skills": ["java"] },
			{ "id": "b", "title": "B", "summary": "B", "difficulty": 2, "core": true, "skills": ["sql"] },
			{ "id": "c", "title": "C", "summary": "C", "difficulty": 3, "core": true, "skills": ["spring"] }""";

	private static final String DEFAULTS = """
			{ "id": "d", "title": "D", "summary": "D", "difficulty": 2, "core": false, "skills": ["debugging"] },
			{ "id": "e", "title": "E", "summary": "E", "difficulty": 2, "core": false, "skills": ["api-design"] }""";

	@Test
	void javaSpringCatalogIsValid() {
		var catalog = MissionCatalog.fromClasspath("missions/java-spring.json", VOCABULARY);

		assertThat(catalog.archetype()).isEqualTo("java-spring");
		assertThat(catalog.domain().id()).isEqualTo("clinic-scheduling");
		assertThat(catalog.missions()).filteredOn(Mission::core)
			.extracting(Mission::id)
			.containsExactly("patients-api", "persistence", "scheduling-rules");
	}

	@Test
	void minimalValidCatalog() {
		var catalog = read(catalog(CORE + "," + DEFAULTS, "[\"d\", \"e\"]"));

		assertThat(catalog.missions()).hasSize(5);
		assertThat(catalog.defaultMissions()).extracting(Mission::id).containsExactly("d", "e");
	}

	@Test
	void rejectsArchetypeThatDoesNotMatchTheVocabulary() {
		assertInvalid(catalog(CORE + "," + DEFAULTS, "[\"d\", \"e\"]").replace("java-spring", "python-django"),
				"catalog archetype python-django does not match vocabulary java-spring");
	}

	@Test
	void rejectsDuplicateMissionId() {
		assertInvalid(catalog(CORE + "," + DEFAULTS + "," + DEFAULTS, "[\"d\", \"e\"]"), "duplicate mission: d");
	}

	@Test
	void rejectsUnknownSkill() {
		assertInvalid(catalog(CORE + "," + DEFAULTS.replace("debugging", "kotlin"), "[\"d\", \"e\"]"),
				"mission d points to an unknown skill: kotlin");
	}

	@Test
	void rejectsMissionWithoutSkills() {
		assertInvalid(catalog(CORE + "," + DEFAULTS.replace("[\"debugging\"]", "[]"), "[\"d\", \"e\"]"),
				"mission d has no skills");
	}

	@Test
	void rejectsDifficultyOutOfRange() {
		assertInvalid(catalog(CORE + "," + DEFAULTS.replace("\"difficulty\": 2, \"core\": false, \"skills\": [\"debugging\"]",
				"\"difficulty\": 5, \"core\": false, \"skills\": [\"debugging\"]"), "[\"d\", \"e\"]"),
				"mission d has a difficulty outside 1-4: 5");
	}

	@Test
	void rejectsCoreMissionTooHardForJuniorPlans() {
		assertInvalid(catalog(CORE.replace("\"difficulty\": 3, \"core\": true", "\"difficulty\": 4, \"core\": true") + ","
				+ DEFAULTS, "[\"d\", \"e\"]"), "core mission c is harder than the junior cap of 3");
	}

	@Test
	void rejectsTooFewCoreMissions() {
		String twoCore = CORE.replace("\"difficulty\": 3, \"core\": true", "\"difficulty\": 3, \"core\": false");
		assertInvalid(catalog(twoCore + "," + DEFAULTS, "[\"d\", \"e\", \"c\"]"), "catalog needs 3 core missions, found 2");
	}

	@Test
	void rejectsDefaultMissionThatIsUnknownOrCore() {
		assertInvalid(catalog(CORE + "," + DEFAULTS, "[\"d\", \"x\", \"a\"]"), "default mission x is unknown",
				"default mission a is core");
	}

	@Test
	void rejectsTooFewDefaultsToReachTheMinimumPlanSize() {
		assertInvalid(catalog(CORE + "," + DEFAULTS, "[\"d\"]"), "catalog needs at least 2 default missions, found 1");
	}

	private static String catalog(String missions, String defaults) {
		return """
				{ "archetype": "java-spring",
				  "domain": { "id": "clinic", "name": "Clínica", "description": "Clínica" },
				  "missions": [ %s ],
				  "defaultMissions": %s }""".formatted(missions, defaults);
	}

	private static void assertInvalid(String json, String... problems) {
		assertThatThrownBy(() -> read(json)).isInstanceOf(InvalidMissionCatalogException.class)
			.satisfies(e -> assertThat(((InvalidMissionCatalogException) e).problems()).contains(problems));
	}

	private static MissionCatalog read(String json) {
		return MissionCatalog.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), VOCABULARY);
	}

}
