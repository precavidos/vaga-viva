package io.github.precavidos.vagaviva.jobposting;

import org.springframework.stereotype.Service;

import io.github.precavidos.vagaviva.archetype.ArchetypeCatalog;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/**
 * Reads a job posting's text and returns what was recognized in it. The text
 * only lives in memory during the call: it is neither stored nor logged
 * (ADR 0003).
 */
@Service
public class JobPostingExtractor {

	/** Maximum length of a posting's text; real postings stay well below it. */
	public static final int MAX_LENGTH = 20_000;

	private final SkillVocabulary vocabulary;

	private final ArchetypeCatalog archetypes;

	JobPostingExtractor(SkillVocabulary vocabulary, ArchetypeCatalog archetypes) {
		this.vocabulary = vocabulary;
		this.archetypes = archetypes;
	}

	public ExtractedJobPosting extract(String text) {
		return new ExtractedJobPosting(vocabulary.archetype(), archetypes.match(text), SeniorityDetector.detect(text),
				vocabulary.recognize(text));
	}

}
