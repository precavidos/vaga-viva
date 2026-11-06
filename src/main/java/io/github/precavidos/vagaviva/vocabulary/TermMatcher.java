package io.github.precavidos.vagaviva.vocabulary;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Finds terms in a text as whole words ("Java" does not match "JavaScript"),
 * ignoring case, accents and extra whitespace. The text must already be in
 * {@link TextNormalizer} form.
 */
public final class TermMatcher {

	private final Map<String, Pattern> patterns = new ConcurrentHashMap<>();

	/** The terms, in their given order, that occur in {@code normalizedText}. */
	public List<String> match(List<String> terms, String normalizedText) {
		return terms.stream().filter(term -> patternFor(term).matcher(normalizedText).find()).toList();
	}

	private Pattern patternFor(String term) {
		return patterns.computeIfAbsent(term, t -> Pattern
			.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(TextNormalizer.normalize(t)) + "(?![\\p{L}\\p{N}])"));
	}

}
