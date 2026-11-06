package io.github.precavidos.vagaviva.writing;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Language model settings. A blank {@code apiKey} disables Claude and the
 * default texts are used. {@code timeout} applies to each attempt.
 */
@ConfigurationProperties("vagaviva.llm")
public record LlmProperties(@DefaultValue("") String apiKey,
		@DefaultValue("claude-haiku-5-5") String model,
		@DefaultValue("30s") Duration timeout,
		@DefaultValue("0") int maxRetries) {

	/** Never shows the API key. */
	@Override
	public String toString() {
		return "LlmProperties[apiKey=" + (apiKey.isBlank() ? "<blank>" : "****") + ", model=" + model + ", timeout="
				+ timeout + ", maxRetries=" + maxRetries + "]";
	}

}
