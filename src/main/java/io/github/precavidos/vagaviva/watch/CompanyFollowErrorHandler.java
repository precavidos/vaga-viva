package io.github.precavidos.vagaviva.watch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * A rejected body holds a career page URL. The answer and the log never repeat
 * it (ADR 0003).
 */
@RestControllerAdvice(assignableTypes = { CompanyFollowController.class, PostingTextController.class })
@Order(Ordered.HIGHEST_PRECEDENCE)
class CompanyFollowErrorHandler {

	private static final Logger log = LoggerFactory.getLogger(CompanyFollowErrorHandler.class);

	@ExceptionHandler({ MethodArgumentNotValidException.class, HttpMessageNotReadableException.class })
	ProblemDetail invalidBody() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Confira o link e o nome (até " + CompanyFollowController.MAX_NAME_LENGTH + " letras) e tente de novo.");
	}

	@ExceptionHandler(RuntimeException.class)
	ProblemDetail unexpected(RuntimeException ex) {
		log.error("Unexpected error in the company watch: {}", ex.getClass().getName());
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
				"Erro inesperado. Tente de novo daqui a pouco.");
	}

}
