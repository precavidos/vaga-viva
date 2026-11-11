package io.github.precavidos.vagaviva.writing;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;

import io.github.precavidos.vagaviva.vocabulary.Vocabularies;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LlmProperties.class)
class LlmConfig {

	@Bean
	MissionTextWriter missionTextWriter(LlmProperties properties, Vocabularies vocabularies) {
		if (properties.apiKey().isBlank()) {
			return new DefaultMissionTextWriter();
		}
		var client = AnthropicOkHttpClient.builder()
			.apiKey(properties.apiKey())
			.timeout(properties.timeout())
			.maxRetries(properties.maxRetries())
			.build();
		return new ClaudeMissionTextWriter(client, properties.model(), vocabularies);
	}

}
