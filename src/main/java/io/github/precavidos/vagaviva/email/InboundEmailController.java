package io.github.precavidos.vagaviva.email;

import java.time.Clock;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Receives Resend's {@code email.received} webhook. Every call must carry a
 * valid Svix signature. The work runs before the answer, so a Resend failure
 * becomes a 503 and Resend sends the event again. Nothing about the e-mail is
 * logged (ADR 0003).
 */
@RestController
class InboundEmailController {

	static final String PATH = "/api/inbound-email";

	private static final Logger log = LoggerFactory.getLogger(InboundEmailController.class);

	private final ResendProperties properties;

	private final InboundEmailService service;

	private final JsonMapper json;

	private final Clock clock;

	InboundEmailController(ResendProperties properties, InboundEmailService service, JsonMapper json, Clock clock) {
		this.properties = properties;
		this.service = service;
		this.json = json;
		this.clock = clock;
	}

	@PostMapping(PATH)
	ResponseEntity<Void> receive(@RequestHeader(name = "svix-id", required = false) String id,
			@RequestHeader(name = "svix-timestamp", required = false) String timestamp,
			@RequestHeader(name = "svix-signature", required = false) String signature,
			@RequestBody(required = false) byte[] body) {
		if (!properties.inboundEnabled()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
		}
		byte[] payload = body == null ? new byte[0] : body;
		if (!SvixSignature.isValid(properties.webhookSecret(), id, timestamp, payload, signature, clock.instant())) {
			log.warn("Rejected an inbound e-mail with an invalid signature");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		try {
			JsonNode event = json.readTree(payload);
			if (!"email.received".equals(event.path("type").asString(""))) {
				return ResponseEntity.ok().build();
			}
			var emailId = event.path("data").path("email_id").asString("");
			if (emailId.isBlank()) {
				return ResponseEntity.ok().build();
			}
			var outcome = service.handle(id, emailId);
			log.info("Inbound e-mail {}: {}", id, outcome.name().toLowerCase(Locale.ROOT).replace('_', '-'));
			return ResponseEntity.ok().build();
		}
		catch (ResendApiException e) {
			log.warn("Inbound e-mail {}: failed ({})", id, e.status());
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
		}
		catch (RuntimeException e) {
			// Messages of Jackson or RestClient exceptions can carry e-mail content (ADR 0003).
			log.warn("Inbound e-mail {}: failed ({})", id, e.getClass().getSimpleName());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}
