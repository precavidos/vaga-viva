package io.github.precavidos.vagaviva.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A repository as returned by the GitHub API. */
public record GitHubRepository(String name, @JsonProperty("full_name") String fullName,
		@JsonProperty("html_url") String htmlUrl, Owner owner) {

	public String ownerLogin() {
		return owner == null ? null : owner.login();
	}

	public record Owner(String login) {
	}

}
