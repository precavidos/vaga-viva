package io.github.precavidos.vagaviva.github;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * GitHub settings. Each archetype's template repository comes from the
 * archetype catalog ({@code archetypes/catalog.json}).
 */
@ConfigurationProperties("vagaviva.github")
public record GitHubProperties(@DefaultValue("https://api.github.com") String apiUrl) {
}
