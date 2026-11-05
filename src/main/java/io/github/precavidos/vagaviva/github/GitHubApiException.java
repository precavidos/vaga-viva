package io.github.precavidos.vagaviva.github;

/** A non-successful GitHub API response. Never carries the token used in the request. */
public class GitHubApiException extends RuntimeException {

	private final int status;

	public GitHubApiException(int status, String message) {
		super("GitHub API responded " + status + (message == null ? "" : ": " + message));
		this.status = status;
	}

	public int status() {
		return status;
	}

}
