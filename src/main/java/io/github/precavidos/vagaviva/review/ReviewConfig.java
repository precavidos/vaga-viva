package io.github.precavidos.vagaviva.review;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.writing.LlmProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GitHubAppProperties.class)
class ReviewConfig {

	/**
	 * The private key is parsed on first use, so a missing or broken key only
	 * stops the webhook, not the whole application.
	 */
	@Bean
	InstallationTokens installationTokens(GitHubApi github, GitHubAppProperties app) {
		Clock clock = Clock.systemUTC();
		var jwt = new AtomicReference<AppJwt>();
		Supplier<String> appJwt = () -> jwt.updateAndGet(current -> current != null ? current
				: new AppJwt(app.id(), app.privateKey(), clock)).create();
		return new InstallationTokens(github, appJwt, clock);
	}

	/** Without an API key, the reviewer posts its fixed texts (the same switch as the student texts). */
	@Bean
	ReviewWriter reviewWriter(LlmProperties llm) {
		if (llm.apiKey().isBlank()) {
			return (context, focus, failing, contract) -> Optional.empty();
		}
		var client = AnthropicOkHttpClient.builder()
			.apiKey(llm.apiKey())
			.timeout(llm.timeout())
			.maxRetries(llm.maxRetries())
			.build();
		return new ClaudeReviewWriter(client, llm.model());
	}

	/** Without an API key, the hints are the fixed texts of each level. */
	@Bean
	HintWriter hintWriter(LlmProperties llm) {
		if (llm.apiKey().isBlank()) {
			return (context, mission, level, failing, contract, earlierHints) -> Optional.empty();
		}
		var client = AnthropicOkHttpClient.builder()
			.apiKey(llm.apiKey())
			.timeout(llm.timeout())
			.maxRetries(llm.maxRetries())
			.build();
		return new ClaudeHintWriter(client, llm.model());
	}

}
