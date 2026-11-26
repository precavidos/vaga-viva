package io.github.precavidos.vagaviva.jobposting;

import java.util.Optional;

/**
 * Reads a career page that the app's own download could not: one built by
 * JavaScript, which reaches {@link JobPostingFetcher} as an empty shell, or a
 * site that refuses the download. A service renders the page in a real browser
 * and gives back its text. Without one configured, {@link #NONE} answers.
 */
interface RenderedPageReader {

	/** No rendering service: the page stays unreadable, as before. */
	RenderedPageReader NONE = url -> Optional.empty();

	/**
	 * The posting's text, at most {@link JobPostingExtractor#MAX_LENGTH}
	 * characters, or empty if the page could not be rendered or holds too
	 * little text. Never throws: a failure here only means the student pastes
	 * the text instead.
	 */
	Optional<String> read(String url);

}
