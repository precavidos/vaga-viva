package io.github.precavidos.vagaviva.email;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * A rejected body holds the typed e-mail address. The answer and the log never
 * repeat it (ADR 0003).
 */
@RestControllerAdvice(assignableTypes = WeeklyEmailController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
class WeeklyEmailErrorHandler {

	@ExceptionHandler({ MethodArgumentNotValidException.class, HttpMessageNotReadableException.class })
	ProblemDetail invalidBody() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Confira o e-mail e tente de novo.");
	}

}
