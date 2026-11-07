package io.github.precavidos.vagaviva.writing;

import java.util.Map;
import java.util.Optional;

/**
 * The texts a writer produced for a plan. Anything missing is replaced by the
 * default text. {@code missionContexts} maps mission ids to their context.
 * {@code usage} is present only when a model was called.
 */
public record WrittenTexts(Optional<String> projectIntro, Map<String, String> missionContexts,
		Optional<TokenUsage> usage) {

	public WrittenTexts {
		missionContexts = Map.copyOf(missionContexts);
	}

	/** Nothing written: every text falls back to the default. */
	public static WrittenTexts none() {
		return new WrittenTexts(Optional.empty(), Map.of(), Optional.empty());
	}

}
