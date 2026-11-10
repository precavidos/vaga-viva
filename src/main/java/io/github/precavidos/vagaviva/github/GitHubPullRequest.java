package io.github.precavidos.vagaviva.github;

/** The parts of a pull request the reviewer needs. */
public record GitHubPullRequest(int number, String headSha) {
}
