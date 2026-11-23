package io.github.precavidos.vagaviva.provisioning;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * The MVP is for people aged 18 or older: minors bring the ECA Digital (Lei 15.211/2025) obligations, which are on
 * the roadmap and not built yet. Every route that creates a student repository requires the student to confirm it.
 */
public final class AdultsOnly {

	public static final String MESSAGE = "O Vaga Viva é para maiores de 18 anos. Confirme que você tem 18 anos ou mais para criar o repositório.";

	private AdultsOnly() {
	}

	/** The 400 answer when the student did not confirm being an adult. */
	public static ResponseEntity<ProblemDetail> refused() {
		return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, MESSAGE));
	}

}
