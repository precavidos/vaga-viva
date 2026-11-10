package io.github.precavidos.vagaviva.github;

/** A file read from a repository: its UTF-8 content and the blob sha GitHub needs to update it. */
public record GitHubFile(String content, String sha) {
}
