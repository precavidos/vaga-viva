package io.github.precavidos.vagaviva.jobposting;

/** Every page-reading slot stayed busy for longer than {@link PageReadLimiter} waits. */
final class TooManyPageReadsException extends RuntimeException {

	TooManyPageReadsException() {
		super("too many job posting pages being read at once", null, false, false);
	}

}
