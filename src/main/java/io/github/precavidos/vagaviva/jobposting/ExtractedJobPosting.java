package io.github.precavidos.vagaviva.jobposting;

import java.util.List;

import io.github.precavidos.vagaviva.archetype.ArchetypeFit;
import io.github.precavidos.vagaviva.vocabulary.Recognition;

/**
 * What remains of a job posting after reading it: the text itself is not part
 * of it. {@code archetypeFit} tells how well the archetype covers the stack the
 * posting names.
 */
public record ExtractedJobPosting(String archetype, ArchetypeFit archetypeFit, List<SeniorityLevel> seniorityLevels,
		Recognition recognition) {

	public ExtractedJobPosting {
		seniorityLevels = List.copyOf(seniorityLevels);
	}

}
