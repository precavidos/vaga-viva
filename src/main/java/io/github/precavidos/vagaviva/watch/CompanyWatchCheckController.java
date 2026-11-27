package io.github.precavidos.vagaviva.watch;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * The scheduled GitHub Actions workflow calls this every Sunday night, with a
 * shared secret instead of a session. A blank secret turns the route off
 * (404); without Firecrawl it answers 503.
 */
@RestController
class CompanyWatchCheckController {

	static final String PATH = "/api/company-watch/checks";

	private final CompanyWatch watch;

	private final CompanyWatchCheck check;

	CompanyWatchCheckController(CompanyWatch watch, CompanyWatchCheck check) {
		this.watch = watch;
		this.check = check;
	}

	@PostMapping(PATH)
	ResponseEntity<CompanyWatchCheck.Summary> run(
			@RequestHeader(name = "X-Vaga-Viva-Secret", required = false) String secret) {
		String expected = watch.secret();
		if (expected.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		if (secret == null || !MessageDigest.isEqual(secret.getBytes(UTF_8), expected.getBytes(UTF_8))) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		if (!watch.listingsEnabled()) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
		}
		return ResponseEntity.ok(check.run());
	}

}
