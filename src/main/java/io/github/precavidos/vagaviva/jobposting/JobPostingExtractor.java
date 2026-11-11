package io.github.precavidos.vagaviva.jobposting;

import org.springframework.stereotype.Service;

import io.github.precavidos.vagaviva.archetype.ArchetypeCatalog;
import io.github.precavidos.vagaviva.archetype.ArchetypeFit;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;
import io.github.precavidos.vagaviva.vocabulary.Vocabularies;

/**
 * Reads a job posting's text and returns what was recognized in it. The text
 * only lives in memory during the call: it is neither stored nor logged
 * (ADR 0003).
 */
@Service
public class JobPostingExtractor {

	/** Maximum length of a posting's text; real postings stay well below it. */
	public static final int MAX_LENGTH = 20_000;

	private final Vocabularies vocabularies;

	private final ArchetypeCatalog archetypes;

	public JobPostingExtractor(Vocabularies vocabularies, ArchetypeCatalog archetypes) {
		this.vocabularies = vocabularies;
		this.archetypes = archetypes;
	}

	/**
	 * The archetype comes first, from the languages and frameworks the posting
	 * names; the skills are then read with that archetype's vocabulary.
	 */
	public ExtractedJobPosting extract(String text) {
		ArchetypeFit fit = archetypes.match(text);
		SkillVocabulary vocabulary = vocabularies.get(fit.archetype());
		return new ExtractedJobPosting(fit.archetype(), fit, SeniorityDetector.detect(text), vocabulary.recognize(text));
	}

}
