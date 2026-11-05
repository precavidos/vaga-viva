package io.github.precavidos.vagaviva.vocabulary;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Loads the vocabulary at startup: an invalid file prevents the application from booting. */
@Configuration(proxyBeanMethods = false)
class VocabularyConfig {

	@Bean
	SkillVocabulary javaSpringVocabulary() {
		return SkillVocabulary.fromClasspath("vocabulary/java-spring.json");
	}

}
