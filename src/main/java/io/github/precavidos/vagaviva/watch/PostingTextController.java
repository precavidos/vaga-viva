package io.github.precavidos.vagaviva.watch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.precavidos.vagaviva.jobposting.JobPostingReader;

/**
 * The text of a posting from the weekly e-mail, so the start page can plan it:
 * the button "Montar meu plano para esta vaga" opens {@code /#link=<url>}.
 * Only posting addresses of the known platforms are read, so this is no
 * general page reader. The URL and the text are neither stored nor logged
 * (ADR 0003).
 */
@RestController
class PostingTextController {

	static final String PATH = "/api/company-watch/posting-text";

	private final CompanyWatch watch;

	private final JobPostingReader reader;

	PostingTextController(CompanyWatch watch, JobPostingReader reader) {
		this.watch = watch;
		this.reader = reader;
	}

	@PostMapping(PATH)
	ResponseEntity<?> text(@Valid @RequestBody TextRequest request) {
		if (!watch.enabled()) {
			return ResponseEntity.notFound().build();
		}
		if (!CareerPageAddress.isKnownPosting(request.url())) {
			return problem(HttpStatus.BAD_REQUEST, "Este link não é de uma vaga das empresas que o Vaga Viva acompanha.");
		}
		return reader.read(request.url())
			.<ResponseEntity<?>>map(text -> ResponseEntity.ok(new TextResponse(text)))
			.orElseGet(() -> problem(HttpStatus.UNPROCESSABLE_CONTENT,
					"Não conseguimos abrir essa vaga agora; ela pode ter sido encerrada. Abra o link da vaga e cole o texto aqui."));
	}

	private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}

	record TextRequest(@NotBlank @Size(max = 2048) String url) {
	}

	record TextResponse(String text) {
	}

}
