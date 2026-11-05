package io.github.precavidos.vagaviva.github;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * GitHub settings. {@code templateRepository} is the archetype's template
 * repository, as {@code owner/name}.
 */
@ConfigurationProperties("vagaviva.github")
public record GitHubProperties(@DefaultValue("https://api.github.com") String apiUrl,
		@DefaultValue("precavidos/vaga-viva-template-java-spring") String templateRepository) {
}
