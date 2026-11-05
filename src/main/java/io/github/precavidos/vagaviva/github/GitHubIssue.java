package io.github.precavidos.vagaviva.github;

import com.fasterxml.jackson.annotation.JsonProperty;

/** An issue as returned by the GitHub API. */
public record GitHubIssue(int number, @JsonProperty("html_url") String htmlUrl) {
}
