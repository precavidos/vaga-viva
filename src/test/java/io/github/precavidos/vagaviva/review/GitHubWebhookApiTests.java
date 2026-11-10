package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

/** GitHub reaches the webhook with no session, login or CSRF token: only the signature counts. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "vagaviva.github.app.id=1", "vagaviva.github.app.private-key=unused-in-this-test",
		"vagaviva.github.app.webhook-secret=test-secret" })
@AutoConfigureMockMvc
class GitHubWebhookApiTests {

	private static final String BODY = "{\"action\":\"completed\"}";

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	WebhookDispatcher dispatcher;

	@Test
	void acceptsASignedDeliveryWithoutLoginOrCsrf() {
		var response = mvc.post()
			.uri("/api/github/webhook")
			.header("X-GitHub-Event", "workflow_run")
			.header("X-GitHub-Delivery", "delivery-1")
			.header("X-Hub-Signature-256", WebhookSignature.sign("test-secret", BODY.getBytes(StandardCharsets.UTF_8)))
			.contentType(MediaType.APPLICATION_JSON)
			.content(BODY)
			.exchange();

		assertThat(response).hasStatus(HttpStatus.ACCEPTED);
		verify(dispatcher, timeout(5_000)).dispatch(eq("workflow_run"), any());
	}

	@Test
	void rejectsAnUnsignedDelivery() {
		var response = mvc.post()
			.uri("/api/github/webhook")
			.header("X-GitHub-Event", "workflow_run")
			.contentType(MediaType.APPLICATION_JSON)
			.content(BODY)
			.exchange();

		assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
	}

}
