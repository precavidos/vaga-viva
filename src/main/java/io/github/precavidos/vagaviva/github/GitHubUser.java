package io.github.precavidos.vagaviva.github;

/** The user a token belongs to. Only the login is kept (ADR 0003). */
public record GitHubUser(String login) {
}
