package io.github.precavidos.vagaviva.vocabulary;

import java.util.List;

/**
 * What the vocabulary recognized in a text. Lists follow vocabulary order, and
 * each item keeps the terms from the text that evidence it.
 */
public record Recognition(List<RecognizedTechnology> technologies, List<RecognizedSkill> skills) {

	public Recognition {
		technologies = List.copyOf(technologies);
		skills = List.copyOf(skills);
	}

	public record RecognizedTechnology(NamedTechnology technology, List<String> matchedTerms) {

		public RecognizedTechnology {
			matchedTerms = List.copyOf(matchedTerms);
		}

	}

	/**
	 * A skill recognized by its own terms or by a technology that evidences it;
	 * {@code evidence} holds the terms that led to it.
	 */
	public record RecognizedSkill(Skill skill, List<String> evidence) {

		public RecognizedSkill {
			evidence = List.copyOf(evidence);
		}

	}

}
