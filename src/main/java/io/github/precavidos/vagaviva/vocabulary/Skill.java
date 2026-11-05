package io.github.precavidos.vagaviva.vocabulary;

import java.util.List;

/**
 * A skill in an archetype's closed vocabulary. It comes from ESCO when
 * {@code esco} is present; otherwise it belongs to the project's custom list.
 */
public record Skill(String id, String label, Esco esco, List<String> terms) {

	public enum Source {
		ESCO, CUSTOM
	}

	public Skill {
		terms = terms == null ? List.of() : List.copyOf(terms);
	}

	public Source source() {
		return esco == null ? Source.CUSTOM : Source.ESCO;
	}

	/** Reference to an ESCO skill, with its official labels. */
	public record Esco(String uri, String preferredLabelPt, String preferredLabelEn) {

		static final String URI_PREFIX = "http://data.europa.eu/esco/skill/";

		boolean hasValidUri() {
			return uri != null && uri.startsWith(URI_PREFIX) && uri.length() > URI_PREFIX.length();
		}

	}

}
