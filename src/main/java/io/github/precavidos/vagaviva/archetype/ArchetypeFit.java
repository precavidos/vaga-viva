package io.github.precavidos.vagaviva.archetype;

import java.util.List;

/**
 * How well an archetype covers the stack a job posting names, so the student
 * is told when the project will not practice what the posting asks for.
 *
 * @param archetype the archetype chosen for the posting
 * @param status how well it covers the posting's stack
 * @param covered names of the posting's languages and frameworks the archetype practices
 * @param uncovered names of the posting's languages and frameworks it does not
 */
public record ArchetypeFit(String archetype, Status status, List<String> covered, List<String> uncovered) {

	public ArchetypeFit {
		covered = List.copyOf(covered);
		uncovered = List.copyOf(uncovered);
	}

	public enum Status {

		/** Everything the posting names is practiced. */
		COVERED,

		/** The archetype's language is in the posting, but so is something it does not practice. */
		PARTIAL,

		/** The posting names languages, none of which has an archetype. */
		NOT_COVERED,

		/** The posting names no language or framework; the default archetype is used. */
		NOT_STATED

	}

}
