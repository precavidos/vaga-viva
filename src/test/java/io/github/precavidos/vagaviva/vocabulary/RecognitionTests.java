package io.github.precavidos.vagaviva.vocabulary;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RecognitionTests {

	private static final SkillVocabulary VOCABULARY = SkillVocabulary.fromClasspath("vocabulary/java-spring.json");

	@Test
	void recognizesTechnologiesAndTheSkillsTheyEvidence() {
		var r = VOCABULARY.recognize("Desenvolvedor Java com Spring Boot e PostgreSQL");

		assertThat(r.technologies()).extracting(t -> t.technology().id())
			.containsExactly("java", "spring-boot", "postgresql");
		assertThat(r.skills()).extracting(s -> s.skill().id())
			.containsExactlyInAnyOrder("java", "spring", "postgresql", "sql");
	}

	@Test
	void javaDoesNotMatchJavaScript() {
		var r = VOCABULARY.recognize("Experiência com JavaScript, TypeScript e Javascript moderno");

		assertThat(r.technologies()).isEmpty();
		assertThat(r.skills()).isEmpty();
	}

	@Test
	void matchesWholeWordsSeparatedByPunctuation() {
		var r = VOCABULARY.recognize("Stack: Java/Spring, (Docker).");

		assertThat(r.technologies()).extracting(t -> t.technology().id()).containsExactly("java", "docker");
	}

	@Test
	void rejectedTermOnItsOwnIsNotRecognized() {
		var r = VOCABULARY.recognize("Spring 2027 internship. The rest of the team has solid experience.");

		assertThat(r.technologies()).isEmpty();
		assertThat(r.skills()).isEmpty();
	}

	@Test
	void ignoresCaseAccentsAndExtraWhitespace() {
		var r = VOCABULARY.recognize("SPRING   BOOT\ne testes unitarios");

		assertThat(r.technologies()).extracting(t -> t.technology().id()).containsExactly("spring-boot");
		assertThat(r.skills()).extracting(s -> s.skill().id())
			.containsExactlyInAnyOrder("spring", "automated-testing");
	}

	@Test
	void sqlDoesNotMatchInsideNoSqlOrMySql() {
		var r = VOCABULARY.recognize("Conhecimento em NoSQL e MySQL");

		assertThat(r.technologies()).isEmpty();
	}

	@Test
	void recognizesTermsWithSlashes() {
		var r = VOCABULARY.recognize("Pipelines de CI/CD");

		assertThat(r.skills()).extracting(s -> s.skill().id()).containsExactly("continuous-integration");
	}

	@Test
	void keepsTheEvidenceForEachSkill() {
		var r = VOCABULARY.recognize("Usamos GitHub Actions e testes automatizados com JUnit");

		assertThat(r.technologies()).extracting(t -> t.technology().id())
			.containsExactly("git", "junit", "ci-tools");
		assertThat(r.skills()).filteredOn(s -> s.skill().id().equals("automated-testing"))
			.singleElement()
			.satisfies(s -> assertThat(s.evidence()).containsExactly("testes automatizados", "JUnit"));
		assertThat(r.technologies()).filteredOn(t -> t.technology().id().equals("git"))
			.singleElement()
			.satisfies(t -> assertThat(t.matchedTerms()).containsExactly("GitHub"));
	}

	@Test
	void emptyTextRecognizesNothing() {
		var r = VOCABULARY.recognize("");

		assertThat(r.technologies()).isEmpty();
		assertThat(r.skills()).isEmpty();
	}

}
