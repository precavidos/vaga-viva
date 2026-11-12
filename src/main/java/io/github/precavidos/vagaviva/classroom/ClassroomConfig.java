package io.github.precavidos.vagaviva.classroom;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ClassroomProperties.class)
class ClassroomConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
