package io.github.precavidos.vagaviva.vocabulary;

import java.util.List;

public class InvalidVocabularyException extends RuntimeException {

	private final List<String> problems;

	InvalidVocabularyException(String archetype, List<String> problems) {
		super("Invalid vocabulary " + archetype + ":\n- " + String.join("\n- ", problems));
		this.problems = List.copyOf(problems);
	}

	public List<String> problems() {
		return problems;
	}

}
