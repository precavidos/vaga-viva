package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class WeeklyEmailUnsubscribeApiTests {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	WeeklyEmailSubscriptionRepository subscriptions;

	@BeforeEach
	void seed() {
		subscriptions.deleteAll();
		WeeklyEmailSubscription row = new WeeklyEmailSubscription("maria");
		row.consent("maria@example.com", "maria/vaga-viva-x", "tok", Instant.parse("2026-11-01T00:00:00Z"));
		subscriptions.save(row);
	}

	@Test
	void deletesTheRowWithTheJsonToken() {
		var response = mvc.post()
			.uri("/api/weekly-email/unsubscribe")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"tok\"}")
			.exchange();

		assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(subscriptions.findAll()).isEmpty();
	}

	@Test
	void oneClickUnsubscribeWorks() {
		var response = mvc.post()
			.uri("/api/weekly-email/unsubscribe?t=tok")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.content("List-Unsubscribe=One-Click")
			.exchange();

		assertThat(response).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(subscriptions.findAll()).isEmpty();
	}

	@Test
	void unknownTokenStillAnswers204() {
		var json = mvc.post()
			.uri("/api/weekly-email/unsubscribe")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"nope\"}")
			.exchange();
		var oneClick = mvc.post()
			.uri("/api/weekly-email/unsubscribe?t=nope")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.content("List-Unsubscribe=One-Click")
			.exchange();
		var empty = mvc.post()
			.uri("/api/weekly-email/unsubscribe")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}")
			.exchange();

		assertThat(json).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(oneClick).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(empty).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(subscriptions.findAll()).hasSize(1);
	}

	@Test
	void needsNoSessionNorCsrf() {
		// The two tests above already send neither oauth2Login() nor csrf(); this pins it for both forms.
		assertThat(mvc.post()
			.uri("/api/weekly-email/unsubscribe?t=tok")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.content("List-Unsubscribe=One-Click")
			.exchange()).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.post()
			.uri("/api/weekly-email/unsubscribe")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"token\":\"tok\"}")
			.exchange()).hasStatus(HttpStatus.NO_CONTENT);
	}

	@Test
	void pageDeletesNothingByItself() {
		var response = mvc.get().uri("/descadastrar?t=tok").exchange();

		assertThat(response).hasStatus(HttpStatus.OK).hasForwardedUrl("/descadastrar.html");
		assertThat(mvc.get().uri("/descadastrar.html")).hasStatusOk().bodyText().contains("Parar de receber");
		assertThat(subscriptions.findByUnsubscribeToken("tok")).isPresent();
	}

}
