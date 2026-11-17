package io.github.precavidos.vagaviva.webhook;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.function.Function;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;
import io.github.precavidos.vagaviva.email.ResendApi;

/**
 * The two anonymous webhooks answer 413 to a body above the limit before the signature is checked and before the
 * controller runs, even when the oversized body is correctly signed. Everything else is left alone.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "vagaviva.webhook.max-body-size=1KB", "vagaviva.resend.api-key=re_test",
		"vagaviva.resend.webhook-secret=whsec_MfKQ9r8GKYqrTwjUPD8ILPZIo2LaLaSw",
		"vagaviva.resend.from=Vaga Viva <vaga@vagaviva.dev>", "vagaviva.resend.public-url=https://vagaviva.dev",
		"vagaviva.github.app.id=1", "vagaviva.github.app.private-key=key",
		"vagaviva.github.app.webhook-secret=" + WebhookBodyLimitApiTests.GITHUB_SECRET })
@AutoConfigureMockMvc
class WebhookBodyLimitApiTests {

	static final String GITHUB_SECRET = "github-secret";

	private static final String RESEND_SECRET = "MfKQ9r8GKYqrTwjUPD8ILPZIo2LaLaSw";

	private static final String BIG = "x".repeat(2_000);

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	ResendApi resend;

	@Test
	void inboundEmailRejectsAnOversizedSignedBody() {
		var json = "{\"type\":\"email.sent\",\"pad\":\"" + BIG + "\"}";

		assertThat(inbound("evt-big", json)).hasStatus(HttpStatus.CONTENT_TOO_LARGE);
		verifyNoInteractions(resend);
	}

	@Test
	void inboundEmailStillAcceptsASignedBodyWithinTheLimit() {
		assertThat(inbound("evt-small", "{\"type\":\"email.sent\"}")).hasStatus(HttpStatus.OK);
	}

	@Test
	void githubWebhookRejectsAnOversizedSignedBody() {
		var body = "{\"action\":\"completed\",\"pad\":\"" + BIG + "\"}";

		// A controller run would answer 202 to this correctly signed delivery.
		assertThat(github(body)).hasStatus(HttpStatus.CONTENT_TOO_LARGE);
	}

	@Test
	void githubWebhookStillAcceptsASignedBodyWithinTheLimit() {
		assertThat(github("{\"action\":\"completed\"}")).hasStatus(HttpStatus.ACCEPTED);
	}

	@Test
	void otherEndpointsAreNotLimited() {
		var response = mvc.post()
			.uri("/api/project-plans")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"pad\":\"" + BIG + "\"}")
			.exchange();

		assertThat(response.getResponse().getStatus()).isNotEqualTo(HttpStatus.CONTENT_TOO_LARGE.value());
	}

	private MvcTestResult inbound(String id, String json) {
		var timestamp = String.valueOf(Instant.now().getEpochSecond());
		return mvc.post()
			.uri("/api/inbound-email")
			.header("svix-id", id)
			.header("svix-timestamp", timestamp)
			.header("svix-signature", "v1," + hmac(Base64.getDecoder().decode(RESEND_SECRET),
					id + "." + timestamp + "." + json, Base64.getEncoder()::encodeToString))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json)
			.exchange();
	}

	private MvcTestResult github(String json) {
		return mvc.post()
			.uri("/api/github/webhook")
			.header("X-GitHub-Event", "ping")
			.header("X-GitHub-Delivery", "d-" + json.length())
			.header("X-Hub-Signature-256",
					"sha256=" + hmac(GITHUB_SECRET.getBytes(UTF_8), json, HexFormat.of()::formatHex))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json)
			.exchange();
	}

	private static String hmac(byte[] key, String text, Function<byte[], String> encoder) {
		try {
			var mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			return encoder.apply(mac.doFinal(text.getBytes(UTF_8)));
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
