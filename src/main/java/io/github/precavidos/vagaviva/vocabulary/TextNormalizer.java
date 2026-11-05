package io.github.precavidos.vagaviva.vocabulary;

import java.text.Normalizer;
import java.util.Locale;

/** Common form for comparing text: no accents, lower case and single spaces. */
public final class TextNormalizer {

	private TextNormalizer() {
	}

	public static String normalize(String text) {
		String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return withoutAccents.toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
	}

}
