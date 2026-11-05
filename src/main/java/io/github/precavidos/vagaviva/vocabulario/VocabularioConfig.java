package io.github.precavidos.vagaviva.vocabulario;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Carrega o vocabulário na subida da aplicação: um arquivo inválido impede o boot. */
@Configuration(proxyBeanMethods = false)
class VocabularioConfig {

	@Bean
	VocabularioDeCompetencias vocabularioJavaSpring() {
		return VocabularioDeCompetencias.doClasspath("vocabulario/java-spring.json");
	}

}
