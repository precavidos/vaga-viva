package io.github.precavidos.vagaviva.provisioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;
import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubApiException;
import io.github.precavidos.vagaviva.github.GitHubIssue;
import io.github.precavidos.vagaviva.github.GitHubRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class StudentRepositoryApiTests {

	/** The access token {@code oauth2Login()} puts in the authorized client. */
	private static final String TOKEN = "access-token";

	private static final String POSTING = """
			{ "text": "Desenvolvedora Java Júnior com Spring Boot, PostgreSQL e Docker. Contato: rh@empresa.example" }""";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	ClientRegistrationRepository registrations;

	@MockitoBean
	GitHubApi github;

	@Test
	void creatingARepositoryRequiresSigningIn() {
		var response = mvc.post()
			.uri("/api/student-repositories")
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(POSTING)
			.exchange();

		assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(github, never()).generateFromTemplate(anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void meTellsWhoIsSignedIn() {
		assertThat(mvc.get().uri("/api/me")).hasStatus(HttpStatus.UNAUTHORIZED);

		var response = mvc.get().uri("/api/me").with(student()).exchange();

		assertThat(response).hasStatusOk();
		assertThat(response).bodyJson().extractingPath("$.login").isEqualTo("maria");
	}

	@Test
	void sendsTheStudentToInstallTheAppWhenItIsMissing() {
		given(github.isAppInstalledOn(TOKEN, "maria")).willReturn(false);

		var response = mvc.post()
			.uri("/api/student-repositories")
			.with(student())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(POSTING)
			.exchange();

		assertThat(response).hasStatus(HttpStatus.CONFLICT);
		assertThat(response).bodyJson()
			.extractingPath("$.installUrl")
			.isEqualTo("https://github.com/apps/vaga-viva/installations/new");
		verify(github, never()).generateFromTemplate(anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void createsTheRepositoryInTheStudentsAccount(CapturedOutput log) {
		given(github.isAppInstalledOn(TOKEN, "maria")).willReturn(true);
		given(github.generateFromTemplate(eq(TOKEN), anyString(), eq("maria"), anyString(), anyString())).willAnswer(
				(call) -> new GitHubRepository(call.getArgument(3), "maria/" + call.getArgument(3),
						"https://github.com/maria/" + call.getArgument(3), new GitHubRepository.Owner("maria")));
		given(github.createIssue(eq(TOKEN), anyString(), anyString(), anyString()))
			.willReturn(new GitHubIssue(1, "https://github.com/maria/repo/issues/1"));

		var response = mvc.post()
			.uri("/api/student-repositories")
			.with(student())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(POSTING)
			.exchange();

		assertThat(response).hasStatus(HttpStatus.CREATED);
		var json = assertThat(response).bodyJson();
		json.extractingPath("$.fullName").asString().startsWith("maria/");
		json.extractingPath("$.issues").asArray().isNotEmpty();
		verify(github).generateFromTemplate(eq(TOKEN), eq("precavidos/vaga-viva-template-java-spring"), eq("maria"),
				anyString(), anyString());
		assertThat(log).doesNotContain("rh@empresa.example").doesNotContain(TOKEN);
	}

	@Test
	void createsATypeScriptPostingFromTheTypeScriptTemplate() {
		given(github.isAppInstalledOn(TOKEN, "maria")).willReturn(true);
		given(github.generateFromTemplate(eq(TOKEN), anyString(), eq("maria"), anyString(), anyString())).willAnswer(
				(call) -> new GitHubRepository(call.getArgument(3), "maria/" + call.getArgument(3),
						"https://github.com/maria/" + call.getArgument(3), new GitHubRepository.Owner("maria")));
		given(github.createIssue(eq(TOKEN), anyString(), anyString(), anyString()))
			.willReturn(new GitHubIssue(1, "https://github.com/maria/repo/issues/1"));

		var response = mvc.post()
			.uri("/api/student-repositories")
			.with(student())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "text": "Vaga júnior TypeScript com NestJS e PostgreSQL" }""")
			.exchange();

		assertThat(response).hasStatus(HttpStatus.CREATED);
		verify(github).generateFromTemplate(eq(TOKEN), eq("precavidos/vaga-viva-template-typescript-node"), eq("maria"),
				anyString(), anyString());
		var body = org.mockito.ArgumentCaptor.forClass(String.class);
		verify(github, org.mockito.Mockito.atLeastOnce()).createIssue(eq(TOKEN), anyString(), anyString(), body.capture());
		assertThat(body.getValue()).contains("npm test -- missions/").doesNotContain("mvnw");
	}

	@Test
	void rejectsTheCallWithoutTheCsrfToken() {
		var response = mvc.post()
			.uri("/api/student-repositories")
			.with(student())
			.contentType(MediaType.APPLICATION_JSON)
			.content(POSTING)
			.exchange();

		assertThat(response).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void anExpiredGitHubTokenAsksToSignInAgain() {
		given(github.isAppInstalledOn(any(), any())).willThrow(new GitHubApiException(401, "Bad credentials"));

		var response = mvc.post()
			.uri("/api/student-repositories")
			.with(student())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(POSTING)
			.exchange();

		assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(response).bodyJson().extractingPath("$.detail").asString().contains("Entre de novo");
	}

	@Test
	void planningStaysOpenWithoutSignInOrCsrf() {
		var response = mvc.post().uri("/api/project-plans").contentType(MediaType.APPLICATION_JSON).content(POSTING).exchange();

		assertThat(response).hasStatusOk();
	}

	private OAuth2LoginRequestPostProcessor student() {
		return oauth2Login().clientRegistration(registrations.findByRegistrationId("github"))
			.attributes((attributes) -> {
				attributes.put("id", 42);
				attributes.put("login", "maria");
			});
	}

}
