package io.github.precavidos.vagaviva.github;

/** An artifact uploaded by a workflow run. */
public record GitHubArtifact(long id, String name, boolean expired, long sizeInBytes) {
}
