package io.github.precavidos.vagaviva.github;

/** The GitHub operations Vaga Viva uses, each made with the student's token. */
public interface GitHubApi {

	/** Creates {@code owner/name} from the configured template repository. */
	GitHubRepository generateFromTemplate(String token, String owner, String name, String description);

	/**
	 * The sha of the newest commit on the default branch. GitHub answers 409
	 * while the repository is still empty, as it is until the template copy
	 * lands.
	 */
	String latestCommitSha(String token, String repositoryFullName);

	/** Creates a file on the repository's default branch. */
	void createFile(String token, String repositoryFullName, String path, String commitMessage, String content);

	GitHubIssue createIssue(String token, String repositoryFullName, String title, String body);

	GitHubUser currentUser(String token);

	/**
	 * Whether the Vaga Viva app is installed on {@code login}'s own account, so
	 * the token can create repositories there.
	 */
	boolean isAppInstalledOn(String token, String login);

}
