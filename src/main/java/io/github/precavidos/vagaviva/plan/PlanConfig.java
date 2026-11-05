package io.github.precavidos.vagaviva.plan;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/** Loads the mission catalog at startup: an invalid catalog prevents the application from booting. */
@Configuration(proxyBeanMethods = false)
class PlanConfig {

	@Bean
	MissionCatalog javaSpringMissionCatalog(SkillVocabulary vocabulary) {
		return MissionCatalog.fromClasspath("missions/java-spring.json", vocabulary);
	}

	@Bean
	ProjectPlanner projectPlanner(MissionCatalog catalog) {
		return new ProjectPlanner(catalog);
	}

}
