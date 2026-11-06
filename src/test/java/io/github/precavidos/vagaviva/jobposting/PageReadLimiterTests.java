package io.github.precavidos.vagaviva.jobposting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class PageReadLimiterTests {

	private final PageReadLimiter limiter = new PageReadLimiter(1, Duration.ofMillis(100));

	@Test
	void runsWorkWhileAPermitIsFree() {
		assertThat(limiter.call(() -> "ok")).isEqualTo("ok");
	}

	@Test
	void refusesWhenAllPermitsAreBusy() throws Exception {
		var started = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		CompletableFuture<String> busy = CompletableFuture.supplyAsync(() -> limiter.call(() -> {
			started.countDown();
			await(release);
			return "primeira";
		}));
		assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

		assertThatThrownBy(() -> limiter.call(() -> "segunda")).isInstanceOf(TooManyPageReadsException.class);

		release.countDown();
		assertThat(busy.get(2, TimeUnit.SECONDS)).isEqualTo("primeira");
		assertThat(limiter.call(() -> "terceira")).isEqualTo("terceira");
	}

	@Test
	void releasesThePermitWhenWorkFails() {
		assertThatThrownBy(() -> limiter.call(() -> {
			throw new PageFetchException();
		})).isInstanceOf(PageFetchException.class);

		assertThat(limiter.call(() -> "ok")).isEqualTo("ok");
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

}
