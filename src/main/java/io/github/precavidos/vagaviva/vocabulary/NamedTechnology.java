package io.github.precavidos.vagaviva.vocabulary;

import java.util.List;

/**
 * A technology recognized in a job posting only by its own name or by the
 * listed terms, linked to the skills it evidences.
 */
public record NamedTechnology(String id, String name, List<String> terms, List<String> skills) {

	public NamedTechnology {
		terms = terms == null ? List.of() : List.copyOf(terms);
		skills = skills == null ? List.of() : List.copyOf(skills);
	}

}
