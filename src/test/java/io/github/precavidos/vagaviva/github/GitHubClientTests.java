package io.github.precavidos.vagaviva.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GitHubClientTests {

	private static final String API = "https://api.github.test";

	private static final String TOKEN = "ghu_test_token";

	private MockRestServiceServer github;

	private GitHubClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		github = MockRestServiceServer.bindTo(builder).build();
		client = new GitHubClient(builder, new GitHubProperties(API, "precavidos/template"));
	}

	@Test
	void generatesARepositoryFromTheTemplateInTheStudentsAccount() {
		github.expect(requestTo(API + "/repos/precavidos/template/generate"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("Authorization", "Bearer " + TOKEN))
			.andExpect(header("Accept", "application/vnd.github+json"))
			.andExpect(header("X-GitHub-Api-Version", "2022-11-28"))
			.andExpect(content().json("""
					{ "owner": "maria", "name": "vaga-viva-clinica", "description": "Projeto",
					  "private": false, "include_all_branches": false }""", JsonCompareMode.STRICT))
			.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
					{ "name": "vaga-viva-clinica", "full_name": "maria/vaga-viva-clinica",
					  "html_url": "https://github.com/maria/vaga-viva-clinica", "owner": { "login": "maria" } }"""));

		GitHubRepository repository = client.generateFromTemplate(TOKEN, "maria", "vaga-viva-clinica", "Projeto");

		assertThat(repository.fullName()).isEqualTo("maria/vaga-viva-clinica");
		assertThat(repository.htmlUrl()).isEqualTo("https://github.com/maria/vaga-viva-clinica");
		assertThat(repository.ownerLogin()).isEqualTo("maria");
		github.verify();
	}

	@Test
	void writesAFileEncodedInBase64() {
		String fileContent = "{ \"missions\": [\"patients-api\"] }\n";
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/contents/.vaga-viva/plan.json"))
			.andExpect(method(HttpMethod.PUT))
			.andExpect(header("Authorization", "Bearer " + TOKEN))
			.andExpect(content().json("""
					{ "message": "Add plan", "content": "%s" }""".formatted(
					Base64.getEncoder().encodeToString(fileContent.getBytes(StandardCharsets.UTF_8)))))
			.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("{}"));

		client.createFile(TOKEN, "maria/vaga-viva-clinica", ".vaga-viva/plan.json", "Add plan", fileContent);

		github.verify();
	}

	@Test
	void readsAFileDecodingBase64() {
		String encoded = Base64.getEncoder().encodeToString("Olá\n".getBytes(StandardCharsets.UTF_8));
		String wrapped = encoded.substring(0, 4) + "\\n" + encoded.substring(4);
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/contents/README.md"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer " + TOKEN))
			.andRespond(withSuccess("""
					{ "type": "file", "content": "%s\\n", "sha": "abc123", "encoding": "base64" }""".formatted(wrapped),
					MediaType.APPLICATION_JSON));

		GitHubFile file = client.readFile(TOKEN, "maria/vaga-viva-clinica", "README.md").orElseThrow();

		assertThat(file.content()).isEqualTo("Olá\n");
		assertThat(file.sha()).isEqualTo("abc123");
		github.verify();
	}

	@Test
	void readingAMissingFileGivesNothing() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/contents/README.md"))
			.andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body("""
					{ "message": "Not Found" }"""));

		assertThat(client.readFile(TOKEN, "maria/vaga-viva-clinica", "README.md")).isEmpty();
	}

	@Test
	void readingADirectoryFails() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/contents/README.md"))
			.andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.readFile(TOKEN, "maria/vaga-viva-clinica", "README.md"))
			.isInstanceOfSatisfying(GitHubApiException.class, e -> assertThat(e.status()).isEqualTo(422));
	}

	@Test
	void readingAnObjectThatIsNotAFileFails() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/contents/README.md"))
			.andRespond(withSuccess("{\"type\":\"dir\"}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.readFile(TOKEN, "maria/vaga-viva-clinica", "README.md"))
			.isInstanceOfSatisfying(GitHubApiException.class, e -> assertThat(e.status()).isEqualTo(422));
	}

	@Test
	void readingAFileWithoutContentFails() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/contents/README.md"))
			.andRespond(withSuccess("{\"type\":\"file\",\"sha\":\"abc\",\"content\":null}",
					MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.readFile(TOKEN, "maria/vaga-viva-clinica", "README.md"))
			.isInstanceOfSatisfying(GitHubApiException.class, e -> assertThat(e.status()).isEqualTo(422));
	}

	@Test
	void readingAFileTooLargeForTheApiFails() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/contents/README.md"))
			.andRespond(withSuccess("{\"type\":\"file\",\"sha\":\"abc\",\"content\":\"\",\"encoding\":\"none\"}",
					MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.readFile(TOKEN, "maria/vaga-viva-clinica", "README.md"))
			.isInstanceOfSatisfying(GitHubApiException.class, e -> assertThat(e.status()).isEqualTo(422));
	}

	@Test
	void updatesAFileWithItsSha() {
		String fileContent = "# Vaga Viva\n";
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/contents/README.md"))
			.andExpect(method(HttpMethod.PUT))
			.andExpect(header("Authorization", "Bearer " + TOKEN))
			.andExpect(content().json("""
					{ "message": "Update", "content": "%s", "sha": "abc123" }""".formatted(
					Base64.getEncoder().encodeToString(fileContent.getBytes(StandardCharsets.UTF_8))),
					JsonCompareMode.STRICT))
			.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		client.updateFile(TOKEN, "maria/vaga-viva-clinica", "README.md", "Update", fileContent, "abc123");

		github.verify();
	}

	@Test
	void createsAnIssue() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/issues"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().json("""
					{ "title": "Missão 1", "body": "Corpo" }"""))
			.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
					{ "number": 7, "html_url": "https://github.com/maria/vaga-viva-clinica/issues/7" }"""));

		GitHubIssue issue = client.createIssue(TOKEN, "maria/vaga-viva-clinica", "Missão 1", "Corpo");

		assertThat(issue.number()).isEqualTo(7);
		assertThat(issue.htmlUrl()).isEqualTo("https://github.com/maria/vaga-viva-clinica/issues/7");
	}

	@Test
	void readsTheAuthenticatedUser() {
		github.expect(requestTo(API + "/user"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
					{ "login": "maria", "id": 42 }""", MediaType.APPLICATION_JSON));

		assertThat(client.currentUser(TOKEN).login()).isEqualTo("maria");
	}

	@Test
	void readsTheLatestCommitOfTheDefaultBranch() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/commits?per_page=1"))
			.andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("""
					[ { "sha": "abc123", "commit": { "message": "Initial commit" } } ]""", MediaType.APPLICATION_JSON));

		assertThat(client.latestCommitSha(TOKEN, "maria/vaga-viva-clinica")).isEqualTo("abc123");
	}

	@Test
	void anEmptyRepositoryHasNoLatestCommit() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/commits?per_page=1"))
			.andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body("""
					{ "message": "Git Repository is empty." }"""));

		assertThatThrownBy(() -> client.latestCommitSha(TOKEN, "maria/vaga-viva-clinica"))
			.isInstanceOfSatisfying(GitHubApiException.class, e -> assertThat(e.status()).isEqualTo(409));
	}

	@Test
	void exchangesTheAppJwtForAnInstallationToken() {
		github.expect(requestTo(API + "/app/installations/99/access_tokens"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("Authorization", "Bearer app.jwt"))
			.andRespond(withSuccess("""
					{ "token": "ghs_installation", "expires_at": "2026-11-10T13:00:00Z" }""", MediaType.APPLICATION_JSON));

		InstallationToken token = client.createInstallationToken("app.jwt", 99);

		assertThat(token.token()).isEqualTo("ghs_installation");
		assertThat(token.expiresAt()).isEqualTo(java.time.Instant.parse("2026-11-10T13:00:00Z"));
	}

	@Test
	void readsAPullRequestAndItsDiff() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/pulls/3"))
			.andExpect(header("Accept", "application/vnd.github+json"))
			.andRespond(withSuccess("""
					{ "number": 3, "head": { "sha": "abc123" } }""", MediaType.APPLICATION_JSON));
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/pulls/3"))
			.andExpect(header("Accept", "application/vnd.github.diff"))
			.andRespond(withSuccess("diff --git a/x b/x", MediaType.parseMediaType("application/vnd.github.diff")));

		assertThat(client.pullRequest(TOKEN, "maria/vaga-viva-clinica", 3)).isEqualTo(new GitHubPullRequest(3, "abc123"));
		assertThat(client.pullRequestDiff(TOKEN, "maria/vaga-viva-clinica", 3)).isEqualTo("diff --git a/x b/x");
	}

	@Test
	void listsTheWorkflowRunsOfACommitAndTheirArtifacts() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/actions/runs?per_page=20&head_sha=abc123"))
			.andRespond(withSuccess("""
					{ "workflow_runs": [ { "id": 7, "name": "Vaga Viva", "status": "completed", "conclusion": "failure",
					                       "head_sha": "abc123" } ] }""", MediaType.APPLICATION_JSON));
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/actions/runs/7/artifacts?per_page=100"))
			.andRespond(withSuccess("""
					{ "artifacts": [ { "id": 11, "name": "surefire-reports", "expired": false, "size_in_bytes": 2048 } ] }""",
					MediaType.APPLICATION_JSON));

		assertThat(client.workflowRuns(TOKEN, "maria/vaga-viva-clinica", "abc123"))
			.containsExactly(new GitHubWorkflowRun(7, "Vaga Viva", "completed", "failure", "abc123"));
		assertThat(client.runArtifacts(TOKEN, "maria/vaga-viva-clinica", 7))
			.containsExactly(new GitHubArtifact(11, "surefire-reports", false, 2048));
	}

	@Test
	void downloadsAnArtifactFromTheStorageUrlWithoutSendingTheToken() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/actions/artifacts/11/zip"))
			.andExpect(header("Authorization", "Bearer " + TOKEN))
			.andRespond(withStatus(HttpStatus.FOUND).location(java.net.URI.create("https://storage.test/artifact.zip?sig=x")));
		github.expect(requestTo("https://storage.test/artifact.zip?sig=x"))
			.andExpect(request -> assertThat(request.getHeaders().containsHeader("Authorization")).isFalse())
			.andRespond(withSuccess(new byte[] { 'P', 'K' }, MediaType.APPLICATION_OCTET_STREAM));

		assertThat(client.downloadArtifact(TOKEN, "maria/vaga-viva-clinica", 11)).containsExactly('P', 'K');
	}

	@Test
	void readsTheCommentsAndTellsBotsApart() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/issues/3/comments?per_page=100"))
			.andRespond(withSuccess("""
					[ { "id": 1, "body": "/dica", "user": { "login": "maria", "type": "User" } },
					  { "id": 2, "body": "Pergunta", "user": { "login": "vaga-viva[bot]", "type": "Bot" } } ]""",
					MediaType.APPLICATION_JSON));

		assertThat(client.issueComments(TOKEN, "maria/vaga-viva-clinica", 3)).containsExactly(
				new GitHubComment(1, "maria", false, "/dica"), new GitHubComment(2, "vaga-viva[bot]", true, "Pergunta"));
	}

	@Test
	void findsTheAppInstalledOnTheUsersOwnAccount() {
		github.expect(requestTo(API + "/user/installations?per_page=100"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer " + TOKEN))
			.andRespond(withSuccess("""
					{ "total_count": 2, "installations": [
					  { "id": 1, "account": { "login": "precavidos" } },
					  { "id": 2, "account": { "login": "Maria" } } ] }""", MediaType.APPLICATION_JSON));

		assertThat(client.isAppInstalledOn(TOKEN, "maria")).isTrue();
	}

	@Test
	void appInstalledOnlyOnOtherAccountsDoesNotCount() {
		github.expect(requestTo(API + "/user/installations?per_page=100"))
			.andRespond(withSuccess("""
					{ "total_count": 1, "installations": [ { "id": 1, "account": { "login": "precavidos" } } ] }""",
					MediaType.APPLICATION_JSON));

		assertThat(client.isAppInstalledOn(TOKEN, "maria")).isFalse();
	}

	@Test
	void errorsCarryTheStatusAndGitHubsMessageButNotTheToken() {
		github.expect(requestTo(API + "/repos/maria/vaga-viva-clinica/issues"))
			.andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON).body("""
					{ "message": "Resource not accessible by integration" }"""));

		assertThatThrownBy(() -> client.createIssue(TOKEN, "maria/vaga-viva-clinica", "t", "b"))
			.isInstanceOfSatisfying(GitHubApiException.class, e -> {
				assertThat(e.status()).isEqualTo(403);
				assertThat(e.getMessage()).contains("Resource not accessible by integration").doesNotContain(TOKEN);
			});
	}

	@Test
	void errorsWithoutAJsonBodyStillCarryTheStatus() {
		github.expect(requestTo(API + "/user")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

		assertThatThrownBy(() -> client.currentUser(TOKEN))
			.isInstanceOfSatisfying(GitHubApiException.class, e -> assertThat(e.status()).isEqualTo(502));
	}

}
