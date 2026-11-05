package io.github.precavidos.vagaviva.jobposting;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Input errors for controllers that receive a job posting's text. Spring's default handling logs the rejected
 * value, which here would be the posting's text; these responses neither echo
 * nor log it (ADR 0003). The details are user-facing, so they stay in
 * Portuguese.
 */
@RestControllerAdvice(annotations = ReceivesJobPostingText.class)
class JobPostingErrorHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail invalidText() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"O texto da vaga é obrigatório e deve ter até " + JobPostingExtractor.MAX_LENGTH + " caracteres.");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail unreadableBody() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Corpo da requisição inválido: envie um JSON no formato {\"text\": \"...\"}.");
	}

}
