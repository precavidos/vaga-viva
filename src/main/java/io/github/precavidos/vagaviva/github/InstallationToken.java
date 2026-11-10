package io.github.precavidos.vagaviva.github;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A short-lived token that acts as the app on one installation. Never logged. */
public record InstallationToken(String token, @JsonProperty("expires_at") Instant expiresAt) {

	@Override
	public String toString() {
		return "InstallationToken[expiresAt=" + expiresAt + "]";
	}

}
