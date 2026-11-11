package io.github.precavidos.vagaviva.github;

import java.util.List;
import java.util.Optional;

/** The GitHub operations Vaga Viva uses, each made with the student's token. */
public interface GitHubApi {

	/** Creates {@code owner/name} from {@code templateRepository} ({@code owner/name} of the template). */
	GitHubRepository generateFromTemplate(String token, String templateRepository, String owner, String name,
			String description);

	/**
	 * The sha of the newest commit on the default branch. GitHub answers 409
	 * while the repository is still empty, as it is until the template copy
	 * lands.
	 */
	String latestCommitSha(String token, String repositoryFullName);

	/** Creates a file on the repository's default branch. */
	void createFile(String token, String repositoryFullName, String path, String commitMessage, String content);

	/** Reads a file from the repository's default branch, or nothing if it does not exist. */
	Optional<GitHubFile> readFile(String token, String repositoryFullName, String path);

	/** Replaces a file on the default branch; {@code sha} is the blob sha from {@link #readFile}. */
	void updateFile(String token, String repositoryFullName, String path, String commitMessage, String content,
			String sha);

	GitHubIssue createIssue(String token, String repositoryFullName, String title, String body);

	GitHubUser currentUser(String token);

	/**
	 * Whether the Vaga Viva app is installed on {@code login}'s own account, so
	 * the token can create repositories there.
	 */
	boolean isAppInstalledOn(String token, String login);

	/**
	 * Exchanges the app's own JWT for a token of one installation, which acts
	 * as the app on the student's repositories (webhooks, reviewer comments).
	 */
	InstallationToken createInstallationToken(String appJwt, long installationId);

	GitHubPullRequest pullRequest(String token, String repositoryFullName, int number);

	/** The pull request's diff, as GitHub renders it. */
	String pullRequestDiff(String token, String repositoryFullName, int number);

	/** The comments on an issue or pull request, oldest first (first 100). */
	List<GitHubComment> issueComments(String token, String repositoryFullName, int number);

	/** The workflow runs for a commit, newest first (first 20). */
	List<GitHubWorkflowRun> workflowRuns(String token, String repositoryFullName, String headSha);

	List<GitHubArtifact> runArtifacts(String token, String repositoryFullName, long runId);

	/** The artifact's zip. GitHub redirects to a storage URL, which is fetched without the token. */
	byte[] downloadArtifact(String token, String repositoryFullName, long artifactId);

	/** Posts a comment on an issue or pull request. */
	GitHubComment createIssueComment(String token, String repositoryFullName, int number, String body);

	/** Replaces the text of an existing comment. */
	void updateIssueComment(String token, String repositoryFullName, long commentId, String body);

}
