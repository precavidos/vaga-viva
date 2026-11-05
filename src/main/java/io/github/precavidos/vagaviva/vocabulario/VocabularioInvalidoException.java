package io.github.precavidos.vagaviva.vocabulario;

import java.util.List;

public class VocabularioInvalidoException extends RuntimeException {

	private final List<String> problemas;

	VocabularioInvalidoException(String arquetipo, List<String> problemas) {
		super("Vocabulário " + arquetipo + " inválido:\n- " + String.join("\n- ", problemas));
		this.problemas = List.copyOf(problemas);
	}

	public List<String> problemas() {
		return problemas;
	}

}
