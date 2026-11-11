package io.github.precavidos.vagaviva.archetype;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.archetype.ArchetypeFit.Status;

class ArchetypeCatalogTests {

	private final ArchetypeCatalog catalog = ArchetypeCatalog.fromClasspath("archetypes/catalog.json");

	@Test
	void catalogFileIsValidAndDefaultsToJavaSpring() {
		assertThat(catalog.defaultArchetype()).isEqualTo("java-spring");
	}

	@Test
	void javaAndSpringAreCovered() {
		ArchetypeFit fit = catalog.match("Desenvolvedor Java Júnior com Spring Boot e PostgreSQL.");

		assertThat(fit.archetype()).isEqualTo("java-spring");
		assertThat(fit.status()).isEqualTo(Status.COVERED);
		assertThat(fit.covered()).containsExactly("Java", "Spring Boot");
		assertThat(fit.uncovered()).isEmpty();
	}

	@Test
	void anotherJavaFrameworkIsPartial() {
		ArchetypeFit fit = catalog.match("Vaga Java + Quarkus para microsserviços.");

		assertThat(fit.archetype()).isEqualTo("java-spring");
		assertThat(fit.status()).isEqualTo(Status.PARTIAL);
		assertThat(fit.covered()).containsExactly("Java");
		assertThat(fit.uncovered()).containsExactly("Quarkus");
	}

	@Test
	void frameworkImpliesItsLanguage() {
		ArchetypeFit fit = catalog.match("Backend em Kotlin com Spring Boot.");

		assertThat(fit.status()).isEqualTo(Status.PARTIAL);
		assertThat(fit.covered()).containsExactly("Spring Boot");
		assertThat(fit.uncovered()).containsExactly("Kotlin");
	}

	@Test
	void fullStackPostingIsPartial() {
		ArchetypeFit fit = catalog.match("Fullstack: Java, Spring Boot e React.");

		assertThat(fit.status()).isEqualTo(Status.PARTIAL);
		assertThat(fit.uncovered()).containsExactly("React");
	}

	@Test
	void otherLanguageIsNotCovered() {
		ArchetypeFit fit = catalog.match("Desenvolvedor Python com Django e FastAPI.");

		assertThat(fit.archetype()).isEqualTo("java-spring");
		assertThat(fit.status()).isEqualTo(Status.NOT_COVERED);
		assertThat(fit.covered()).isEmpty();
		assertThat(fit.uncovered()).containsExactly("Python", "Django", "FastAPI");
	}

	@Test
	void javaScriptIsNotJava() {
		ArchetypeFit fit = catalog.match("Back-end JavaScript com Node.js.");

		assertThat(fit.archetype()).isEqualTo("typescript-node");
		assertThat(fit.status()).isEqualTo(Status.COVERED);
		assertThat(fit.covered()).containsExactly("JavaScript", "Node.js");
	}

	@Test
	void typeScriptAndNestJsGetTheTypeScriptArchetype() {
		ArchetypeFit fit = catalog.match("Desenvolvedora júnior TypeScript com NestJS, Prisma e PostgreSQL.");

		assertThat(fit.archetype()).isEqualTo("typescript-node");
		assertThat(fit.status()).isEqualTo(Status.COVERED);
		assertThat(fit.covered()).containsExactly("TypeScript", "NestJS");
	}

	@Test
	void aFrontEndFrameworkIsWhatTheTypeScriptProjectLeavesOut() {
		ArchetypeFit fit = catalog.match("Fullstack TypeScript: NestJS no back-end e React no front-end.");

		assertThat(fit.archetype()).isEqualTo("typescript-node");
		assertThat(fit.status()).isEqualTo(Status.PARTIAL);
		assertThat(fit.uncovered()).containsExactly("React");
	}

	@Test
	void eachArchetypeKnowsItsTemplateAndMissionCommand() {
		var java = catalog.archetype("java-spring");
		var typescript = catalog.archetype("typescript-node");

		assertThat(java.name()).isEqualTo("Java e Spring Boot");
		assertThat(java.templateRepository()).isEqualTo("precavidos/vaga-viva-template-java-spring");
		assertThat(java.missionTestCommand("bug-hunt")).isEqualTo("./mvnw test -Dgroups=bug-hunt");
		assertThat(typescript.name()).isEqualTo("TypeScript e NestJS");
		assertThat(typescript.templateRepository()).isEqualTo("precavidos/vaga-viva-template-typescript-node");
		assertThat(typescript.missionTestCommand("bug-hunt")).isEqualTo("npm test -- missions/bug-hunt");
	}

	@Test
	void postingThatNamesNoLanguageIsNotStated() {
		ArchetypeFit fit = catalog.match("Vaga de estágio em desenvolvimento, com SQL e Git. Vamos juntos, go!");

		assertThat(fit.archetype()).isEqualTo("java-spring");
		assertThat(fit.status()).isEqualTo(Status.NOT_STATED);
		assertThat(fit.covered()).isEmpty();
		assertThat(fit.uncovered()).isEmpty();
	}

	@Test
	void languagesWithSymbolsAreRecognized() {
		assertThat(catalog.match("Experiência com C# e .NET").uncovered()).containsExactly("C#");
		assertThat(catalog.match("Sistemas embarcados em C++").uncovered()).containsExactly("C++");
		assertThat(catalog.match("Serviços em Golang").uncovered()).containsExactly("Go");
	}

	@Test
	void archetypeWithMostCoveredItemsWins() {
		var twoArchetypes = read("""
				{ "technologies": [
				    { "id": "java", "name": "Java", "terms": ["Java"] },
				    { "id": "python", "name": "Python", "terms": ["Python"] },
				    { "id": "fastapi", "name": "FastAPI", "language": "python", "terms": ["FastAPI"] } ],
				  "archetypes": [
				    { "id": "java-spring", "name": "java-spring", "languages": ["java"], "frameworks": [], "templateRepository": "o/java-spring", "missionTestCommand": "t %s" },
				    { "id": "python-fastapi", "name": "python-fastapi", "languages": ["python"], "frameworks": ["fastapi"], "templateRepository": "o/python-fastapi", "missionTestCommand": "t %s" } ] }""");

		ArchetypeFit fit = twoArchetypes.match("Python com FastAPI; Java é um diferencial.");

		assertThat(fit.archetype()).isEqualTo("python-fastapi");
		assertThat(fit.status()).isEqualTo(Status.PARTIAL);
		assertThat(fit.uncovered()).containsExactly("Java");
	}

	@Test
	void tieGoesToTheFirstArchetype() {
		var twoArchetypes = read("""
				{ "technologies": [
				    { "id": "java", "name": "Java", "terms": ["Java"] },
				    { "id": "python", "name": "Python", "terms": ["Python"] } ],
				  "archetypes": [
				    { "id": "java-spring", "name": "java-spring", "languages": ["java"], "frameworks": [], "templateRepository": "o/java-spring", "missionTestCommand": "t %s" },
				    { "id": "python-fastapi", "name": "python-fastapi", "languages": ["python"], "frameworks": [], "templateRepository": "o/python-fastapi", "missionTestCommand": "t %s" } ] }""");

		assertThat(twoArchetypes.match("Python e Java").archetype()).isEqualTo("java-spring");
	}

	@Test
	void invalidCatalogsAreRejected() {
		assertThatThrownBy(() -> read("""
				{ "technologies": [
				    { "id": "quarkus", "name": "Quarkus", "language": "java", "terms": ["Quarkus"] } ],
				  "archetypes": [ { "id": "java-spring", "name": "java-spring", "languages": ["java"], "frameworks": ["quarkus"], "templateRepository": "o/java-spring", "missionTestCommand": "t %s" } ] }"""))
			.isInstanceOf(InvalidArchetypeCatalogException.class)
			.hasMessageContaining("quarkus points to an unknown language: java")
			.hasMessageContaining("archetype java-spring has an unknown language: java");

		assertThatThrownBy(() -> read("""
				{ "technologies": [
				    { "id": "java", "name": "Java", "terms": ["Java"] },
				    { "id": "java2", "name": "Java 2", "terms": ["java"] } ],
				  "archetypes": [ { "id": "java-spring", "name": "java-spring", "languages": ["java"], "frameworks": ["java"], "templateRepository": "o/java-spring", "missionTestCommand": "t %s" } ] }"""))
			.isInstanceOf(InvalidArchetypeCatalogException.class)
			.hasMessageContaining("term \"java\" appears in more than one technology")
			.hasMessageContaining("archetype java-spring lists java, which is not a framework of java");

		assertThatThrownBy(() -> read("""
				{ "technologies": [], "archetypes": [] }"""))
			.isInstanceOf(InvalidArchetypeCatalogException.class)
			.hasMessageContaining("no archetypes");
	}

	private static ArchetypeCatalog read(String json) {
		return ArchetypeCatalog.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
	}

}
