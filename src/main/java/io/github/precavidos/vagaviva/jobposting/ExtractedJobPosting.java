package io.github.precavidos.vagaviva.jobposting;

import java.util.List;

import io.github.precavidos.vagaviva.vocabulary.Recognition;

/** What remains of a job posting after reading it: the text itself is not part of it. */
public record ExtractedJobPosting(String archetype, List<SeniorityLevel> seniorityLevels, Recognition recognition) {

	public ExtractedJobPosting {
		seniorityLevels = List.copyOf(seniorityLevels);
	}

}
