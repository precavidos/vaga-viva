package io.github.precavidos.vagaviva.jobposting;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

/**
 * Caps how many career pages are downloaded and parsed at once. A 2 MiB page
 * can become hundreds of megabytes of parsed HTML, and the endpoint needs no
 * login, so without a cap a handful of simultaneous requests could exhaust the
 * memory of the demo container. Work that cannot get a slot within a short wait
 * is refused rather than queued.
 */
@Component
class PageReadLimiter {

	static final int MAX_CONCURRENT_READS = 3;

	static final Duration WAIT = Duration.ofSeconds(2);

	private final Semaphore slots;

	private final Duration wait;

	PageReadLimiter() {
		this(MAX_CONCURRENT_READS, WAIT);
	}

	PageReadLimiter(int slots, Duration wait) {
		this.slots = new Semaphore(slots);
		this.wait = wait;
	}

	/** Runs {@code work} in a free slot, or throws {@link TooManyPageReadsException}. */
	<T> T call(Supplier<T> work) {
		boolean acquired;
		try {
			acquired = slots.tryAcquire(wait.toNanos(), TimeUnit.NANOSECONDS);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new TooManyPageReadsException();
		}
		if (!acquired) {
			throw new TooManyPageReadsException();
		}
		try {
			return work.get();
		}
		finally {
			slots.release();
		}
	}

}
