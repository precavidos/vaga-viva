package io.github.precavidos.vagaviva.email;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import io.github.precavidos.vagaviva.email.WeeklyEmailService.Summary;
import io.github.precavidos.vagaviva.review.RepositorySnapshots;

/**
 * The scheduled GitHub Actions workflow calls this every Monday, with a shared
 * secret instead of a session.
 */
@RestController
class WeeklyEmailTriggerController {

	static final String PATH = "/api/weekly-emails";

	private final WeeklyEmailProperties weekly;

	private final ResendProperties resend;

	private final RepositorySnapshots snapshots;

	private final WeeklyEmailService service;

	WeeklyEmailTriggerController(WeeklyEmailProperties weekly, ResendProperties resend, RepositorySnapshots snapshots,
			WeeklyEmailService service) {
		this.weekly = weekly;
		this.resend = resend;
		this.snapshots = snapshots;
		this.service = service;
	}

	@PostMapping(PATH)
	ResponseEntity<Summary> trigger(@RequestHeader(name = "X-Vaga-Viva-Secret", required = false) String secret) {
		if (weekly.secret().isBlank() || !resend.sendingEnabled() || !snapshots.enabled()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
		}
		if (secret == null || !MessageDigest.isEqual(secret.getBytes(UTF_8), weekly.secret().getBytes(UTF_8))) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return ResponseEntity.ok(service.sendDue());
	}

}
