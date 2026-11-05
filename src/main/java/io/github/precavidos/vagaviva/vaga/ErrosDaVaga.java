package io.github.precavidos.vagaviva.vaga;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erros de entrada da vaga. O tratamento padrão do Spring registra em log o
 * valor rejeitado, que aqui seria o texto da vaga; estas respostas não ecoam
 * nem registram o texto (ADR 0003).
 */
@RestControllerAdvice(assignableTypes = VagaController.class)
class ErrosDaVaga {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail textoInvalido() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"O texto da vaga é obrigatório e deve ter até " + ExtratorDeVaga.TAMANHO_MAXIMO + " caracteres.");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ProblemDetail corpoIlegivel() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Corpo da requisição inválido: envie um JSON no formato {\"texto\": \"...\"}.");
	}

}
