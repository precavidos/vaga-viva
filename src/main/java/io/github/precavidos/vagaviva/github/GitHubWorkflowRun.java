package io.github.precavidos.vagaviva.github;

/** A GitHub Actions workflow run, as far as the reviewer needs it. */
public record GitHubWorkflowRun(long id, String name, String status, String conclusion, String headSha) {

	public boolean completed() {
		return "completed".equals(status);
	}

}
