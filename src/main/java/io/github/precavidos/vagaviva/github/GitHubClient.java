package io.github.precavidos.vagaviva.github;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
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

	/** Reads a file from the repository's default branch, or nothing if it does not exist. */
	@Override
	public Optional<GitHubFile> readFile(String token, String repositoryFullName, String path) {
		FileResponse response;
		try {
			response = http.get()
				.uri("/repos/" + repositoryFullName + "/contents/" + path)
				.headers(h -> h.setBearerAuth(token))
				.retrieve()
				.body(FileResponse.class);
		}
		catch (GitHubApiException e) {
			if (e.status() == 404) {
				return Optional.empty();
			}
			throw e;
		}
		// GitHub wraps the Base64 content at 60 columns, so the MIME decoder skips the line breaks.
		String content = new String(Base64.getMimeDecoder().decode(response.content()), StandardCharsets.UTF_8);
		return Optional.of(new GitHubFile(content, response.sha()));
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

	/** The parts of GitHub's file response that Vaga Viva reads. */
	private record FileResponse(String content, String sha) {
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
