package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class WeeklyEmailConsentApiTests {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	ClientRegistrationRepository registrations;

	@Autowired
	WeeklyEmailSubscriptionRepository subscriptions;

	@BeforeEach
	void clean() {
		subscriptions.deleteAll();
	}

	@Test
	void savesTheConsentOfTheRepositoryOwner() {
		var response = mvc.post()
			.uri("/api/weekly-email")
			.with(student("Maria"))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body("maria@example.com", "maria/vaga-viva-x"))
			.exchange();

		assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
		WeeklyEmailSubscription row = subscriptions.findById("maria").orElseThrow();
		assertThat(row.getConsentedAt()).isNotNull();
		assertThat(row.getEmail()).isEqualTo("maria@example.com");
		assertThat(row.getRepositoryFullName()).isEqualTo("maria/vaga-viva-x");
		assertThat(row.getUnsubscribeToken()).hasSize(43);
	}

	@Test
	void consentingAgainKeepsOneRowAndRenewsTheToken() {
		for (String repository : new String[] { "maria/vaga-viva-x", "maria/vaga-viva-y" }) {
			assertThat(mvc.post()
				.uri("/api/weekly-email")
				.with(student("maria"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body("maria@example.com", repository))
				.exchange()).hasStatus(HttpStatus.NO_CONTENT);
		}

		assertThat(subscriptions.findAll()).hasSize(1);
		assertThat(subscriptions.findById("maria").orElseThrow().getRepositoryFullName())
			.isEqualTo("maria/vaga-viva-y");
	}

	@Test
	void refusesSomeoneElsesRepository() {
		var response = mvc.post()
			.uri("/api/weekly-email")
			.with(student("Maria"))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body("maria@example.com", "joao/vaga-viva-x"))
			.exchange();

		assertThat(response).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(subscriptions.count()).isZero();
	}

	@Test
	void refusesAnInvalidEmail() {
		var response = mvc.post()
			.uri("/api/weekly-email")
			.with(student("maria"))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body("maria", "maria/vaga-viva-x"))
			.exchange();

		assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(subscriptions.count()).isZero();
	}

	@Test
	void needsASession() {
		var response = mvc.post()
			.uri("/api/weekly-email")
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body("maria@example.com", "maria/vaga-viva-x"))
			.exchange();

		assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void needsCsrf() {
		var response = mvc.post()
			.uri("/api/weekly-email")
			.with(student("maria"))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body("maria@example.com", "maria/vaga-viva-x"))
			.exchange();

		assertThat(response).hasStatus(HttpStatus.FORBIDDEN);
	}

	private static String body(String email, String repository) {
		return "{\"email\":\"%s\",\"repositoryFullName\":\"%s\"}".formatted(email, repository);
	}

	private OAuth2LoginRequestPostProcessor student(String login) {
		return oauth2Login().clientRegistration(registrations.findByRegistrationId("github"))
			.attributes((attributes) -> {
				attributes.put("id", 42);
				attributes.put("login", login);
			});
	}

}
