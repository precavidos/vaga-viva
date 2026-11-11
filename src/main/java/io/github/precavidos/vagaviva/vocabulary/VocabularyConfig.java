package io.github.precavidos.vagaviva.vocabulary;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Loads each archetype's vocabulary at startup: an invalid file prevents the application from booting. */
@Configuration(proxyBeanMethods = false)
class VocabularyConfig {

	@Bean
	Vocabularies vocabularies() {
		return Vocabularies.of(SkillVocabulary.fromClasspath("vocabulary/java-spring.json"),
				SkillVocabulary.fromClasspath("vocabulary/typescript-node.json"));
	}

}
