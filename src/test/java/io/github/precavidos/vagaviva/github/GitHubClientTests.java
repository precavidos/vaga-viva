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
