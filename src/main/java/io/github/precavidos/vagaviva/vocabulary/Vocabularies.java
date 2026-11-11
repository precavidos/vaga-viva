package io.github.precavidos.vagaviva.vocabulary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The skill vocabulary of each archetype, by archetype id. */
public final class Vocabularies {

	private final Map<String, SkillVocabulary> byArchetype;

	private Vocabularies(Map<String, SkillVocabulary> byArchetype) {
		this.byArchetype = Map.copyOf(byArchetype);
	}

	public static Vocabularies of(SkillVocabulary... vocabularies) {
		var map = new LinkedHashMap<String, SkillVocabulary>();
		for (SkillVocabulary vocabulary : vocabularies) {
			if (map.putIfAbsent(vocabulary.archetype(), vocabulary) != null) {
				throw new IllegalArgumentException("Two vocabularies for archetype " + vocabulary.archetype());
			}
		}
		return new Vocabularies(map);
	}

	/** The vocabulary of the archetype; fails for an archetype without one, which is a configuration error. */
	public SkillVocabulary get(String archetype) {
		SkillVocabulary vocabulary = byArchetype.get(archetype);
		if (vocabulary == null) {
			throw new IllegalArgumentException("No vocabulary for archetype " + archetype);
		}
		return vocabulary;
	}

	public List<SkillVocabulary> all() {
		return List.copyOf(byArchetype.values());
	}

}
