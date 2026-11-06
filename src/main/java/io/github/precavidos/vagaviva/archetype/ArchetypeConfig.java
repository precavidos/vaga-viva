package io.github.precavidos.vagaviva.archetype;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Loads the archetype catalog at startup: an invalid catalog prevents the application from booting. */
@Configuration(proxyBeanMethods = false)
class ArchetypeConfig {

	@Bean
	ArchetypeCatalog archetypeCatalog() {
		return ArchetypeCatalog.fromClasspath("archetypes/catalog.json");
	}

}
