package io.github.precavidos.vagaviva.provisioning;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * The 409 answer for a student who has not installed the Vaga Viva app yet; shared
 * by every route that creates a repository.
 */
public final class InstallAppProblem {

	public static final String MESSAGE = "Instale o app do Vaga Viva na sua conta do GitHub para criarmos o seu repositório.";

	private InstallAppProblem() {
	}

	public static ProblemDetail of(String installUrl) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, MESSAGE);
		problem.setProperty("installUrl", installUrl);
		return problem;
	}

}
