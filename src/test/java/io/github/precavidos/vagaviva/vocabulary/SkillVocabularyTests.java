package io.github.precavidos.vagaviva.vocabulary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class SkillVocabularyTests {

	private static final String ESCO_JAVA = """
			{ "uri": "http://data.europa.eu/esco/skill/19a8293b-8e95-4de3-983f-77484079c389",
			  "preferredLabelPt": "Java (programação informática)", "preferredLabelEn": "Java (computer programming)" }""";

	@Test
	void javaSpringArchetypeFileIsValid() {
		var vocabulary = SkillVocabulary.fromClasspath("vocabulary/java-spring.json");

		assertThat(vocabulary.archetype()).isEqualTo("java-spring");
		assertThat(vocabulary.skill("java")).hasValueSatisfying(java -> {
			assertThat(java.source()).isEqualTo(Skill.Source.ESCO);
			assertThat(java.esco().uri()).startsWith("http://data.europa.eu/esco/skill/");
		});
		assertThat(vocabulary.skill("spring"))
			.hasValueSatisfying(spring -> assertThat(spring.source()).isEqualTo(Skill.Source.CUSTOM));
		assertThat(vocabulary.technologies()).extracting(NamedTechnology::id).contains("java", "spring-boot");
	}

	@Test
	void minimalValidVocabulary() {
		var vocabulary = read("""
				{ "archetype": "test",
				  "skills": [ { "id": "java", "label": "Java", "esco": %s, "terms": [] } ],
				  "technologies": [ { "id": "java", "name": "Java", "terms": ["Java"], "skills": ["java"] } ],
				  "rejectedTerms": [] }""".formatted(ESCO_JAVA));

		assertThat(vocabulary.skills()).hasSize(1);
		assertThat(vocabulary.skill("unknown")).isEmpty();
	}

	@Test
	void rejectsDuplicateSkillId() {
		assertInvalid("""
				{ "archetype": "test",
				  "skills": [
				    { "id": "java", "label": "Java", "terms": [] },
				    { "id": "java", "label": "Java again", "terms": [] } ],
				  "technologies": [], "rejectedTerms": [] }""", "duplicate skill: java");
	}

	@Test
	void rejectsTechnologyPointingToUnknownSkill() {
		assertInvalid("""
				{ "archetype": "test",
				  "skills": [ { "id": "java", "label": "Java", "terms": [] } ],
				  "technologies": [ { "id": "kotlin", "name": "Kotlin", "terms": ["Kotlin"], "skills": ["kotlin"] } ],
				  "rejectedTerms": [] }""", "technology kotlin points to an unknown skill: kotlin");
	}

	@Test
	void rejectsTechnologyWithoutSkills() {
		assertInvalid("""
				{ "archetype": "test",
				  "skills": [],
				  "technologies": [ { "id": "java", "name": "Java", "terms": ["Java"], "skills": [] } ],
				  "rejectedTerms": [] }""", "technology java points to no skill");
	}

	@Test
	void rejectsTechnologyWithoutTerms() {
		assertInvalid("""
				{ "archetype": "test",
				  "skills": [ { "id": "java", "label": "Java", "terms": [] } ],
				  "technologies": [ { "id": "java", "name": "Java", "terms": [], "skills": ["java"] } ],
				  "rejectedTerms": [] }""", "technology java has no terms");
	}

	@Test
	void rejectsSameTermInTwoEntriesIgnoringCaseAndAccents() {
		assertInvalid("""
				{ "archetype": "test",
				  "skills": [
				    { "id": "testing", "label": "Testing", "terms": ["Testes unitários"] },
				    { "id": "other", "label": "Other", "terms": [] } ],
				  "technologies": [ { "id": "junit", "name": "JUnit", "terms": ["testes  unitarios"], "skills": ["other"] } ],
				  "rejectedTerms": [] }""", "term \"testes  unitarios\" appears in more than one entry");
	}

	@Test
	void rejectsTermFromTheRejectedList() {
		assertInvalid("""
				{ "archetype": "test",
				  "skills": [ { "id": "spring", "label": "Spring", "terms": [] } ],
				  "technologies": [ { "id": "spring", "name": "Spring", "terms": ["spring"], "skills": ["spring"] } ],
				  "rejectedTerms": [ { "term": "Spring", "reason": "season of the year" } ] }""",
				"rejected term in use: spring (season of the year)");
	}

	@Test
	void rejectsUriThatIsNotAnEscoSkill() {
		assertInvalid("""
				{ "archetype": "test",
				  "skills": [ { "id": "java", "label": "Java",
				    "esco": { "uri": "http://data.europa.eu/esco/occupation/123", "preferredLabelPt": "x", "preferredLabelEn": "x" },
				    "terms": [] } ],
				  "technologies": [], "rejectedTerms": [] }""", "skill java has an invalid ESCO URI");
	}

	@Test
	void rejectsBlankLabel() {
		assertInvalid("""
				{ "archetype": "test",
				  "skills": [ { "id": "java", "label": " ", "terms": [] } ],
				  "technologies": [], "rejectedTerms": [] }""", "skill java has no label");
	}

	@Test
	void reportsAllProblemsAtOnce() {
		assertThatThrownBy(() -> read("""
				{ "archetype": "test",
				  "skills": [ { "id": "java", "label": "", "terms": [] } ],
				  "technologies": [ { "id": "go", "name": "Go", "terms": [], "skills": ["go"] } ],
				  "rejectedTerms": [] }"""))
			.isInstanceOf(InvalidVocabularyException.class)
			.satisfies(e -> assertThat(((InvalidVocabularyException) e).problems()).hasSize(3));
	}

	private static void assertInvalid(String json, String problem) {
		assertThatThrownBy(() -> read(json)).isInstanceOf(InvalidVocabularyException.class)
			.satisfies(e -> assertThat(((InvalidVocabularyException) e).problems()).contains(problem));
	}

	private static SkillVocabulary read(String json) {
		InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
		return SkillVocabulary.read(input);
	}

}
