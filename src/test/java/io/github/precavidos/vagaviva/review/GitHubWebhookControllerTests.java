package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** The webhook endpoint itself: signatures, retries and turning the webhook off. */
class GitHubWebhookControllerTests {

	private static final byte[] BODY = "{\"action\":\"completed\"}".getBytes(StandardCharsets.UTF_8);

	private final WebhookDispatcher dispatcher = mock(WebhookDispatcher.class);

	private final GitHubWebhookController controller = new GitHubWebhookController(
			new GitHubAppProperties("1", "key", "secret", "vaga-viva"), dispatcher, Runnable::run);

	@Test
	void acceptsASignedDeliveryAndDispatchesIt() {
		var response = controller.receive("workflow_run", "d-1", WebhookSignature.sign("secret", BODY), BODY);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		verify(dispatcher).dispatch("workflow_run", BODY);
	}

	@Test
	void rejectsDeliveriesWithAWrongOrMissingSignature() {
		assertThat(controller.receive("workflow_run", "d-1", WebhookSignature.sign("other", BODY), BODY).getStatusCode())
			.isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(controller.receive("workflow_run", "d-2", null, BODY).getStatusCode())
			.isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(dispatcher, never()).dispatch(any(), any());
	}

	@Test
	void processesARedeliveredEventOnlyOnce() {
		String signature = WebhookSignature.sign("secret", BODY);

		controller.receive("workflow_run", "d-1", signature, BODY);
		var again = controller.receive("workflow_run", "d-1", signature, BODY);

		assertThat(again.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		verify(dispatcher, times(1)).dispatch(eq("workflow_run"), any());
	}

	@Test
	void answersServiceUnavailableWhileTheAppIsNotConfigured() {
		var disabled = new GitHubWebhookController(new GitHubAppProperties("", "", "", "vaga-viva"), dispatcher,
				Runnable::run);

		assertThat(disabled.receive("workflow_run", "d-1", "sha256=00", BODY).getStatusCode())
			.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void aFailingDispatchDoesNotFailTheDelivery() {
		org.mockito.Mockito.doThrow(new IllegalStateException("boom")).when(dispatcher).dispatch(any(), any());

		var response = controller.receive("workflow_run", "d-1", WebhookSignature.sign("secret", BODY), BODY);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
	}

}
