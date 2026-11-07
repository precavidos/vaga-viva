package io.github.precavidos.vagaviva.github;

import java.util.Optional;

/** The GitHub operations Vaga Viva uses, each made with the student's token. */
public interface GitHubApi {

	/** Creates {@code owner/name} from the configured template repository. */
	GitHubRepository generateFromTemplate(String token, String owner, String name, String description);

	/** Creates a file on the repository's default branch. */
	void createFile(String token, String repositoryFullName, String path, String commitMessage, String content);

	/** Reads a file from the repository's default branch, or nothing if it does not exist. */
	Optional<GitHubFile> readFile(String token, String repositoryFullName, String path);

	/** Replaces a file on the default branch; {@code sha} is the blob sha from {@link #readFile}. */
	void updateFile(String token, String repositoryFullName, String path, String commitMessage, String content,
			String sha);

	GitHubIssue createIssue(String token, String repositoryFullName, String title, String body);

	GitHubUser currentUser(String token);

}
