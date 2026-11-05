package io.github.precavidos.vagaviva.plan;

import java.util.List;

public class InvalidMissionCatalogException extends RuntimeException {

	private final List<String> problems;

	InvalidMissionCatalogException(String archetype, List<String> problems) {
		super("Invalid mission catalog " + archetype + ":\n- " + String.join("\n- ", problems));
		this.problems = List.copyOf(problems);
	}

	public List<String> problems() {
		return problems;
	}

}
