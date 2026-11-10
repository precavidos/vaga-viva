package io.github.precavidos.vagaviva.github;

/**
 * A comment on an issue or pull request. {@code bot} tells comments written
 * by an app (such as the Vaga Viva reviewer) from the student's.
 */
public record GitHubComment(long id, String authorLogin, boolean bot, String body) {
}
