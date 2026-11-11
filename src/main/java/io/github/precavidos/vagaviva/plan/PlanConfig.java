package io.github.precavidos.vagaviva.plan;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.precavidos.vagaviva.vocabulary.Vocabularies;

/** Loads each archetype's mission catalog at startup: an invalid catalog prevents the application from booting. */
@Configuration(proxyBeanMethods = false)
class PlanConfig {

	@Bean
	MissionCatalog javaSpringMissionCatalog(Vocabularies vocabularies) {
		return MissionCatalog.fromClasspath("missions/java-spring.json", vocabularies.get("java-spring"));
	}

	@Bean
	MissionCatalog typescriptNodeMissionCatalog(Vocabularies vocabularies) {
		return MissionCatalog.fromClasspath("missions/typescript-node.json", vocabularies.get("typescript-node"));
	}

	@Bean
	ProjectPlanners projectPlanners(List<MissionCatalog> catalogs) {
		return new ProjectPlanners(catalogs);
	}

}
