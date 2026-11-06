package io.github.precavidos.vagaviva.jobposting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(JobPostingUrlErrorHandler.class);

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

	@ExceptionHandler(TooManyPageReadsException.class)
	ProblemDetail busy() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
				"Muitas páginas sendo lidas agora. Tente de novo em instantes ou cole o texto da vaga.");
	}

	/**
	 * Safety net for errors nobody expected: Spring would log them with their
	 * message, which may hold the URL or the page, so only the class is logged.
	 */
	@ExceptionHandler(RuntimeException.class)
	ProblemDetail unexpected(RuntimeException ex) {
		log.error("Unexpected error reading a job posting page: {}", ex.getClass().getName());
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"Erro inesperado ao ler a página da vaga. Tente de novo ou cole o texto da vaga.");
	}

}
