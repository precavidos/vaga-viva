package io.github.precavidos.vagaviva.github;

/** The GitHub operations Vaga Viva uses, each made with the student's token. */
public interface GitHubApi {

	/** Creates {@code owner/name} from the configured template repository. */
	GitHubRepository generateFromTemplate(String token, String owner, String name, String description);

	/** Creates a file on the repository's default branch. */
	void createFile(String token, String repositoryFullName, String path, String commitMessage, String content);

	GitHubIssue createIssue(String token, String repositoryFullName, String title, String body);

	GitHubUser currentUser(String token);

}
