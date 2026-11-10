package io.github.precavidos.vagaviva.review;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.InstallationToken;

/**
 * Installation tokens, kept in memory until five minutes before they expire
 * (GitHub issues them for one hour). Nothing is written to disk or logged.
 */
class InstallationTokens {

	private static final Duration MARGIN = Duration.ofMinutes(5);

	private final GitHubApi github;

	/** A fresh app JWT on each call. */
	private final Supplier<String> appJwt;

	private final Clock clock;

	private final Map<Long, InstallationToken> cache = new ConcurrentHashMap<>();

	InstallationTokens(GitHubApi github, Supplier<String> appJwt, Clock clock) {
		this.github = github;
		this.appJwt = appJwt;
		this.clock = clock;
	}

	String forInstallation(long installationId) {
		InstallationToken token = cache.compute(installationId, (id, cached) -> stillValid(cached) ? cached
				: github.createInstallationToken(appJwt.get(), id));
		return token.token();
	}

	private boolean stillValid(InstallationToken token) {
		return token != null && token.expiresAt() != null && clock.instant().plus(MARGIN).isBefore(token.expiresAt());
	}

}
