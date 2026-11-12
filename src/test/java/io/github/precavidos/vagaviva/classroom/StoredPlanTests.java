package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.archetype.ArchetypeCatalog;
import io.github.precavidos.vagaviva.jobposting.JobPostingExtractor;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.plan.ProjectPlanners;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;
import io.github.precavidos.vagaviva.vocabulary.Vocabularies;

class StoredPlanTests {

	private static final SkillVocabulary VOCABULARY = SkillVocabulary.fromClasspath("vocabulary/java-spring.json");

	private static final MissionCatalog CATALOG = MissionCatalog.fromClasspath("missions/java-spring.json", VOCABULARY);

	private static final ProjectPlanners PLANNERS = new ProjectPlanners(List.of(CATALOG));

	private static final JobPostingExtractor EXTRACTOR = new JobPostingExtractor(Vocabularies.of(VOCABULARY),
			ArchetypeCatalog.fromClasspath("archetypes/catalog.json"));

	@Test
	void roundTripsAPlan() {
		var plan = PLANNERS.plan(EXTRACTOR.extract("Vaga Java júnior com Spring Boot, PostgreSQL e Docker"));

		var restored = StoredPlan.fromJson(StoredPlan.of(plan).toJson()).toProjectPlan(CATALOG);

		assertThat(restored).isEqualTo(plan);
		assertThat(StoredPlan.of(plan).missionIds()).isEqualTo(plan.missions().stream().map((m) -> m.mission().id()).toList());
	}

	@Test
	void jsonHasNoPostingWords() {
		var plan = PLANNERS.plan(EXTRACTOR.extract("Vaga Java júnior com Spring Boot. Contato: rh@empresa.example"));

		assertThat(StoredPlan.of(plan).toJson()).doesNotContain("rh@").doesNotContain("empresa");
	}

}
