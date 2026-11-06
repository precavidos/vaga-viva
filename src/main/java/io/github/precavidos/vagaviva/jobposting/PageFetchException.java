package io.github.precavidos.vagaviva.jobposting;

/**
 * The job posting page could not be downloaded. Neither the message nor a cause carries the URL or the page (ADR 0003).
 */
final class PageFetchException extends RuntimeException {

	PageFetchException() {
		super("could not download the job posting page", null, false, false);
	}

}
