package io.github.precavidos.vagaviva.jobposting;

/**
 * The page has no readable job posting text, or is not HTML. Neither the message nor a cause carries the URL or the page (ADR 0003).
 */
final class UnreadablePageException extends RuntimeException {

	UnreadablePageException() {
		super("no job posting text found in the page", null, false, false);
	}

}
