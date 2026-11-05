package io.github.precavidos.vagaviva.plan;

import java.util.List;

/**
 * A unit of work in the template repository, with tests that fail until the
 * student implements it. Title and summary are shown to students, so they are
 * in Portuguese.
 */
public record Mission(String id, String title, String summary, int difficulty, boolean core, List<String> skills) {

	public Mission {
		skills = skills == null ? List.of() : List.copyOf(skills);
	}

}
