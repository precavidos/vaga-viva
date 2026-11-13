package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ResendClientTests {

	private static final String API = "https://api.resend.com";

	private MockRestServiceServer resend;

	private ResendClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		resend = MockRestServiceServer.bindTo(builder).build();
		client = new ResendClient(builder,
				new ResendProperties("re_test", "whsec_x", "Vaga Viva <vagaviva@example.com>", "https://app.example.com", 5, 80));
	}

	@Test
	void readsAReceivedEmail() {
		resend.expect(requestTo(API + "/emails/receiving/abc"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header("Authorization", "Bearer re_test"))
			.andRespond(withSuccess("""
					{"from":"Maria <maria@example.com>","subject":"Fwd: Vaga","message_id":"<m1@x>",
					 "text":"corpo","html":"<p>corpo</p>","headers":{"Auto-Submitted":"no"}}""",
					MediaType.APPLICATION_JSON));

		ReceivedEmail email = client.receivedEmail("abc");

		assertThat(email.from()).isEqualTo("Maria <maria@example.com>");
		assertThat(email.subject()).isEqualTo("Fwd: Vaga");
		assertThat(email.messageId()).isEqualTo("<m1@x>");
		assertThat(email.text()).isEqualTo("corpo");
		assertThat(email.html()).isEqualTo("<p>corpo</p>");
		assertThat(email.headers().get("auto-submitted")).isEqualTo("no");
		resend.verify();
	}

	@Test
	void toleratesMissingFields() {
		resend.expect(requestTo(API + "/emails/receiving/abc"))
			.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		ReceivedEmail email = client.receivedEmail("abc");

		assertThat(email.text()).isEmpty();
		assertThat(email.from()).isEmpty();
		assertThat(email.headers()).isEmpty();
	}

	@Test
	void sendsAnEmail() {
		resend.expect(requestTo(API + "/emails"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("Authorization", "Bearer re_test"))
			.andExpect(header("Idempotency-Key", "inbound/1"))
			.andExpect(content().json("""
					{"from":"Vaga Viva <vagaviva@example.com>","to":["maria@example.com"],"subject":"Seu plano",
					 "text":"oi","html":"<p>oi</p>","headers":{"Auto-Submitted":"auto-replied"}}""",
					JsonCompareMode.STRICT))
			.andRespond(withSuccess("{\"id\":\"x\"}", MediaType.APPLICATION_JSON));

		client.send(new OutgoingEmail("maria@example.com", "Seu plano", "oi", "<p>oi</p>",
				Map.of("Auto-Submitted", "auto-replied"), "inbound/1"));

		resend.verify();
	}

	@Test
	void omitsEmptyHeadersWhenSending() {
		resend.expect(requestTo(API + "/emails"))
			.andExpect(content().json("""
					{"from":"Vaga Viva <vagaviva@example.com>","to":["maria@example.com"],"subject":"s",
					 "text":"t","html":"h"}""", JsonCompareMode.STRICT))
			.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		client.send(new OutgoingEmail("maria@example.com", "s", "t", "h", Map.of(), "weekly/maria/2026-11-09"));

		resend.verify();
	}

	@Test
	void errorsCarryOnlyTheStatus() {
		resend.expect(requestTo(API + "/emails"))
			.andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
				.body("{\"message\":\"maria@example.com is invalid\"}"));

		assertThatThrownBy(() -> client.send(new OutgoingEmail("maria@example.com", "s", "t", "h", Map.of(), "k")))
			.isInstanceOfSatisfying(ResendApiException.class, e -> {
				assertThat(e.status()).isEqualTo(422);
				assertThat(e.getMessage()).isEqualTo("Resend answered 422").doesNotContain("maria");
				assertThat(e.getCause()).isNull();
			});
	}

	@Test
	void networkFailuresBecomeStatusZeroWithoutACause() {
		resend.expect(requestTo(API + "/emails/receiving/abc"))
			.andRespond(request -> {
				throw new IOException("connection reset by maria@example.com");
			});

		assertThatThrownBy(() -> client.receivedEmail("abc")).isInstanceOfSatisfying(ResendApiException.class, e -> {
			assertThat(e.status()).isZero();
			assertThat(e.getCause()).isNull();
			assertThat(e.getMessage()).doesNotContain("maria");
		});
	}

	@Test
	void aMalformedBodyBecomesStatusZeroWithoutACause() {
		resend.expect(requestTo(API + "/emails/receiving/abc"))
			.andRespond(withSuccess("not json maria@example.com", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> client.receivedEmail("abc")).isInstanceOfSatisfying(ResendApiException.class, e -> {
			assertThat(e.status()).isZero();
			assertThat(e.getCause()).isNull();
			assertThat(e.getMessage()).doesNotContain("maria");
		});
	}

	@Test
	void toStringNeverShowsTheKey() {
		assertThat(client.toString()).doesNotContain("re_test");
	}

}
