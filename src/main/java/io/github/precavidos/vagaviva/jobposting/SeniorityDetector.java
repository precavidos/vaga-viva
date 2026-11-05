package io.github.precavidos.vagaviva.jobposting;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.precavidos.vagaviva.vocabulary.TextNormalizer;

/**
 * Detects the seniority requested in a job posting. Like the vocabulary, it
 * prefers missing a level to inventing one: the Portuguese "pleno" (mid-level)
 * only counts next to a job title or another level ("pleno domínio de inglês"
 * means "full command of English"), and "Sr." is left out because in
 * Portuguese it also means "Mister".
 */
final class SeniorityDetector {

	private static final String START = "(?<![\\p{L}\\p{N}])";

	private static final String END = "(?![\\p{L}\\p{N}])";

	/** Portuguese job titles, plus "nivel" (level), that may precede "pleno". */
	private static final String JOB_TITLE = "(?:desenvolvedora?|dev|programadora?|engenheir[oa]|analista|nivel)";

	private static final String SEPARATOR = "[\\s:,\\-]+";

	private static final Map<SeniorityLevel, Pattern> PATTERNS = Map.of(
			SeniorityLevel.INTERN, pattern("(?:estagio|estagiari[oa]|intern|internship)"),
			SeniorityLevel.JUNIOR, pattern("(?:junior|jr)"),
			SeniorityLevel.MID, pattern("(?:mid[\\s-]level"
					+ "|" + JOB_TITLE + "(?:" + SEPARATOR + "[\\p{L}\\p{N}]+){0,2}" + SEPARATOR + "pleno"
					+ "|(?:estagio|junior|senior)\\s*(?:/|ou|e)\\s*pleno"
					+ "|pleno\\s*(?:/|ou|e)\\s*(?:junior|senior))"),
			SeniorityLevel.SENIOR, pattern("senior"));

	private SeniorityDetector() {
	}

	/** The levels mentioned in the posting, lowest first; empty when none is recognized. */
	static List<SeniorityLevel> detect(String text) {
		String normalized = TextNormalizer.normalize(text);
		var levels = EnumSet.noneOf(SeniorityLevel.class);
		PATTERNS.forEach((level, pattern) -> {
			if (pattern.matcher(normalized).find()) {
				levels.add(level);
			}
		});
		return List.copyOf(levels);
	}

	private static Pattern pattern(String expression) {
		return Pattern.compile(START + expression + END);
	}

}
