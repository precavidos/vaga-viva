package io.github.precavidos.vagaviva.review;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives the GitHub App's webhooks. Every delivery must carry a valid
 * {@code X-Hub-Signature-256}; the work runs after the answer, so GitHub gets
 * its 202 well within its 10-second limit. A delivery GitHub sends again (same
 * {@code X-GitHub-Delivery}) is acknowledged and not processed twice.
 */
@RestController
class GitHubWebhookController {

	static final String PATH = "/api/github/webhook";

	private static final int REMEMBERED_DELIVERIES = 1_000;

	private static final Logger log = LoggerFactory.getLogger(GitHubWebhookController.class);

	private final GitHubAppProperties app;

	private final WebhookDispatcher dispatcher;

	private final Executor executor;

	private final Set<String> deliveries = Collections
		.synchronizedSet(Collections.newSetFromMap(new LinkedHashMap<String, Boolean>() {

			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
				return size() > REMEMBERED_DELIVERIES;
			}

		}));

	GitHubWebhookController(GitHubAppProperties app, WebhookDispatcher dispatcher,
			@Qualifier("applicationTaskExecutor") Executor executor) {
		this.app = app;
		this.dispatcher = dispatcher;
		this.executor = executor;
	}

	@PostMapping(PATH)
	ResponseEntity<Void> receive(@RequestHeader(name = "X-GitHub-Event", required = false) String event,
			@RequestHeader(name = "X-GitHub-Delivery", required = false) String delivery,
			@RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
			@RequestBody(required = false) byte[] body) {
		if (!app.webhookEnabled()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
		}
		byte[] payload = body == null ? new byte[0] : body;
		if (!WebhookSignature.isValid(app.webhookSecret(), payload, signature)) {
			log.warn("Rejected a webhook delivery with an invalid signature");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		if (event == null || (delivery != null && !deliveries.add(delivery))) {
			return ResponseEntity.accepted().build();
		}
		executor.execute(() -> {
			try {
				dispatcher.dispatch(event, payload);
			}
			catch (RuntimeException e) {
				log.warn("Webhook {} {} failed: {}", event, delivery, e.getClass().getSimpleName());
			}
		});
		return ResponseEntity.accepted().build();
	}

}
