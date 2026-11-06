package io.github.precavidos.vagaviva.jobposting;

/**
 * The job posting URL is invalid or points to a non-public address. Neither the message nor a cause carries the URL or the page (ADR 0003).
 */
final class UnsafeUrlException extends RuntimeException {

	UnsafeUrlException() {
		super("unsafe or invalid job posting URL", null, false, false);
	}

}
