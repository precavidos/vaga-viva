package io.github.precavidos.vagaviva.github;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The few GitHub REST calls Vaga Viva needs, made with the student's own
 * token. The token is passed per call and never stored or logged here.
 */
@Component
public class GitHubClient implements GitHubApi {

	private static final String API_VERSION = "2022-11-28";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final RestClient http;

	private final String templateRepository;

	public GitHubClient(RestClient.Builder builder, GitHubProperties properties) {
		this.http = builder.baseUrl(properties.apiUrl())
			.defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
			.defaultHeader("X-GitHub-Api-Version", API_VERSION)
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
				throw toException(response);
			})
			.build();
		this.templateRepository = properties.templateRepository();
	}

	/** Creates {@code owner/name} from the configured template repository. */
	@Override
	public GitHubRepository generateFromTemplate(String token, String owner, String name, String description) {
		return http.post()
			.uri("/repos/" + templateRepository + "/generate")
			.headers(h -> h.setBearerAuth(token))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("owner", owner, "name", name, "description", description, "private", false,
					"include_all_branches", false))
			.retrieve()
			.body(GitHubRepository.class);
	}

	@Override
	public String latestCommitSha(String token, String repositoryFullName) {
		JsonNode commits = http.get()
			.uri("/repos/" + repositoryFullName + "/commits?per_page=1")
			.headers(h -> h.setBearerAuth(token))
			.retrieve()
			.body(JsonNode.class);
		if (commits == null || !commits.isArray() || commits.isEmpty()) {
			throw new GitHubApiException(409, "Git Repository is empty.");
		}
		return commits.get(0).path("sha").asString();
	}

	/** Creates a file on the repository's default branch. */
	@Override
	public void createFile(String token, String repositoryFullName, String path, String commitMessage, String content) {
		String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
		http.put()
			.uri("/repos/" + repositoryFullName + "/contents/" + path)
			.headers(h -> h.setBearerAuth(token))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("message", commitMessage, "content", encoded))
			.retrieve()
			.toBodilessEntity();
	}

	/** Reads a file from the repository's default branch, or nothing if it does not exist. Fails with 422 if the path is not a regular file. */
	@Override
	public Optional<GitHubFile> readFile(String token, String repositoryFullName, String path) {
		JsonNode response;
		try {
			response = http.get()
				.uri("/repos/" + repositoryFullName + "/contents/" + path)
				.headers(h -> h.setBearerAuth(token))
				.retrieve()
				.body(JsonNode.class);
		}
		catch (GitHubApiException e) {
			if (e.status() == 404) {
				return Optional.empty();
			}
			throw e;
		}
		// Files over 1 MB come back with "content":"" and "encoding":"none"; treating them as empty would overwrite the README.
		if (response == null || !"file".equals(response.path("type").asString(null))
				|| !response.hasNonNull("content") || !"base64".equals(response.path("encoding").asString(null))) {
			throw new GitHubApiException(422, "Not a regular file");
		}
		// GitHub wraps the Base64 content at 60 columns, so the MIME decoder skips the line breaks.
		String content = new String(Base64.getMimeDecoder().decode(response.get("content").asString()),
				StandardCharsets.UTF_8);
		return Optional.of(new GitHubFile(content, response.path("sha").asString(null)));
	}

	/** Replaces a file on the default branch; {@code sha} is the blob sha from {@link #readFile}. */
	@Override
	public void updateFile(String token, String repositoryFullName, String path, String commitMessage, String content,
			String sha) {
		String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
		http.put()
			.uri("/repos/" + repositoryFullName + "/contents/" + path)
			.headers(h -> h.setBearerAuth(token))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("message", commitMessage, "content", encoded, "sha", sha))
			.retrieve()
			.toBodilessEntity();
	}

	@Override
	public GitHubIssue createIssue(String token, String repositoryFullName, String title, String body) {
		return http.post()
			.uri("/repos/" + repositoryFullName + "/issues")
			.headers(h -> h.setBearerAuth(token))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("title", title, "body", body))
			.retrieve()
			.body(GitHubIssue.class);
	}

	@Override
	public GitHubUser currentUser(String token) {
		return http.get().uri("/user").headers(h -> h.setBearerAuth(token)).retrieve().body(GitHubUser.class);
	}

	/**
	 * Looks through the installations the user's token can see. With a user
	 * token, GitHub lists only installations of this app, so it is enough to
	 * find one on the user's own account.
	 */
	@Override
	public boolean isAppInstalledOn(String token, String login) {
		JsonNode response = http.get()
			.uri("/user/installations?per_page=100")
			.headers(h -> h.setBearerAuth(token))
			.retrieve()
			.body(JsonNode.class);
		if (response == null) {
			return false;
		}
		for (JsonNode installation : response.path("installations")) {
			if (login.equalsIgnoreCase(installation.path("account").path("login").asString(""))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public InstallationToken createInstallationToken(String appJwt, long installationId) {
		return http.post()
			.uri("/app/installations/" + installationId + "/access_tokens")
			.headers(h -> h.setBearerAuth(appJwt))
			.retrieve()
			.body(InstallationToken.class);
	}

	@Override
	public GitHubPullRequest pullRequest(String token, String repositoryFullName, int number) {
		JsonNode response = http.get()
			.uri("/repos/" + repositoryFullName + "/pulls/" + number)
			.headers(h -> h.setBearerAuth(token))
			.retrieve()
			.body(JsonNode.class);
		if (response == null) {
			throw new GitHubApiException(502, "Empty pull request response");
		}
		return new GitHubPullRequest(response.path("number").asInt(number), response.path("head").path("sha").asString());
	}

	@Override
	public String pullRequestDiff(String token, String repositoryFullName, int number) {
		String diff = http.get()
			.uri("/repos/" + repositoryFullName + "/pulls/" + number)
			.headers(h -> {
				h.setBearerAuth(token);
				h.set(HttpHeaders.ACCEPT, "application/vnd.github.diff");
			})
			.retrieve()
			.body(String.class);
		return diff == null ? "" : diff;
	}

	@Override
	public List<GitHubComment> issueComments(String token, String repositoryFullName, int number) {
		JsonNode response = http.get()
			.uri("/repos/" + repositoryFullName + "/issues/" + number + "/comments?per_page=100")
			.headers(h -> h.setBearerAuth(token))
			.retrieve()
			.body(JsonNode.class);
		var comments = new ArrayList<GitHubComment>();
		if (response != null) {
			for (JsonNode comment : response) {
				JsonNode user = comment.path("user");
				comments.add(new GitHubComment(comment.path("id").asLong(), user.path("login").asString(""),
						"Bot".equals(user.path("type").asString("")), comment.path("body").asString("")));
			}
		}
		return comments;
	}

	@Override
	public List<GitHubWorkflowRun> workflowRuns(String token, String repositoryFullName, String headSha) {
		JsonNode response = http.get()
			.uri("/repos/" + repositoryFullName + "/actions/runs?per_page=20&head_sha={sha}", headSha)
			.headers(h -> h.setBearerAuth(token))
			.retrieve()
			.body(JsonNode.class);
		var runs = new ArrayList<GitHubWorkflowRun>();
		if (response != null) {
			for (JsonNode run : response.path("workflow_runs")) {
				runs.add(new GitHubWorkflowRun(run.path("id").asLong(), run.path("name").asString(""),
						run.path("status").asString(""), run.path("conclusion").asString(null),
						run.path("head_sha").asString("")));
			}
		}
		return runs;
	}

	@Override
	public List<GitHubArtifact> runArtifacts(String token, String repositoryFullName, long runId) {
		JsonNode response = http.get()
			.uri("/repos/" + repositoryFullName + "/actions/runs/" + runId + "/artifacts?per_page=100")
			.headers(h -> h.setBearerAuth(token))
			.retrieve()
			.body(JsonNode.class);
		var artifacts = new ArrayList<GitHubArtifact>();
		if (response != null) {
			for (JsonNode artifact : response.path("artifacts")) {
				artifacts.add(new GitHubArtifact(artifact.path("id").asLong(), artifact.path("name").asString(""),
						artifact.path("expired").asBoolean(false), artifact.path("size_in_bytes").asLong()));
			}
		}
		return artifacts;
	}

	/**
	 * Redirects are not followed by the HTTP client ({@code spring.http.clients.redirects}),
	 * so the token is sent to GitHub only, never to the storage host it redirects to.
	 */
	@Override
	public byte[] downloadArtifact(String token, String repositoryFullName, long artifactId) {
		ResponseEntity<byte[]> response = http.get()
			.uri("/repos/" + repositoryFullName + "/actions/artifacts/" + artifactId + "/zip")
			.headers(h -> h.setBearerAuth(token))
			.retrieve()
			.toEntity(byte[].class);
		if (response.getStatusCode().is3xxRedirection() && response.getHeaders().getLocation() != null) {
			byte[] zip = http.get()
				.uri(response.getHeaders().getLocation())
				.header(HttpHeaders.ACCEPT, "*/*")
				.retrieve()
				.body(byte[].class);
			return zip == null ? new byte[0] : zip;
		}
		return response.getBody() == null ? new byte[0] : response.getBody();
	}

	@Override
	public GitHubComment createIssueComment(String token, String repositoryFullName, int number, String body) {
		JsonNode comment = http.post()
			.uri("/repos/" + repositoryFullName + "/issues/" + number + "/comments")
			.headers(h -> h.setBearerAuth(token))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("body", body))
			.retrieve()
			.body(JsonNode.class);
		if (comment == null) {
			throw new GitHubApiException(502, "Empty comment response");
		}
		JsonNode user = comment.path("user");
		return new GitHubComment(comment.path("id").asLong(), user.path("login").asString(""),
				"Bot".equals(user.path("type").asString("")), comment.path("body").asString(""));
	}

	@Override
	public void updateIssueComment(String token, String repositoryFullName, long commentId, String body) {
		http.patch()
			.uri("/repos/" + repositoryFullName + "/issues/comments/" + commentId)
			.headers(h -> h.setBearerAuth(token))
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("body", body))
			.retrieve()
			.toBodilessEntity();
	}

	private static GitHubApiException toException(ClientHttpResponse response) throws java.io.IOException {
		int status = response.getStatusCode().value();
		String message = null;
		try {
			JsonNode body = JSON.readTree(response.getBody());
			if (body != null && body.hasNonNull("message")) {
				message = body.get("message").asString();
			}
		}
		catch (RuntimeException ignored) {
			// No JSON body: the status alone is enough.
		}
		return new GitHubApiException(status, message);
	}

}
