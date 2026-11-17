package io.github.precavidos.vagaviva.classroom;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * The scheduled GitHub Actions workflow calls this every Monday, with a shared secret instead of a session.
 * A blank secret in the configuration turns the route off (404); without e-mail configured it answers 503.
 */
@RestController
class WeeklySummaryController {

	static final String PATH = "/api/classrooms/weekly-summaries";

	private final ClassroomProperties properties;

	private final ClassroomMailer mailer;

	private final WeeklySummaryService service;

	WeeklySummaryController(ClassroomProperties properties, ClassroomMailer mailer, WeeklySummaryService service) {
		this.properties = properties;
		this.mailer = mailer;
		this.service = service;
	}

	@PostMapping(PATH)
	ResponseEntity<WeeklySummaryService.Result> send(
			@RequestHeader(name = "X-Vaga-Viva-Secret", required = false) String secret) {
		String expected = properties.summarySecret().strip();
		if (expected.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		if (secret == null || !MessageDigest.isEqual(secret.getBytes(UTF_8), expected.getBytes(UTF_8))) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		if (!mailer.enabled()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
		}
		return ResponseEntity.ok(service.sendAll());
	}

}
