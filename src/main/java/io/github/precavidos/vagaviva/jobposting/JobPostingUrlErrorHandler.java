package io.github.precavidos.vagaviva.jobposting;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errors of {@link JobPostingUrlController}. Fixed messages that neither echo
 * nor log the URL or the page (ADR 0003). The details are user-facing, so they
 * stay in Portuguese.
 */
@RestControllerAdvice(assignableTypes = JobPostingUrlController.class)
class JobPostingUrlErrorHandler {

	@ExceptionHandler({ MethodArgumentNotValidException.class, UnsafeUrlException.class })
	ProblemDetail invalidUrl() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Informe o endereço público de uma página de vaga, começando com http:// ou https://.");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail unreadableBody() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Corpo da requisição inválido: envie um JSON no formato {\"url\": \"...\"}.");
	}

	@ExceptionHandler(PageFetchException.class)
	ProblemDetail pageNotDownloaded() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
				"Não foi possível baixar a página da vaga. Confira o endereço ou cole o texto da vaga.");
	}

	@ExceptionHandler(UnreadablePageException.class)
	ProblemDetail pageWithoutPosting() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT,
				"Não encontramos o texto da vaga nessa página; ela pode depender de JavaScript. Cole o texto da vaga.");
	}

}
