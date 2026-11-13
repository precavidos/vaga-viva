package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;

class InboundLimitsTests {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-12T10:00:00Z"));

	@Test
	void limitsEachSenderPerHour() {
		var limits = new InboundLimits(5, 80, clock);
		for (int i = 0; i < 5; i++) {
			assertThat(limits.tryAcquire("Maria <MARIA@example.com>")).isTrue();
		}
		assertThat(limits.tryAcquire("Maria <MARIA@example.com>")).isFalse();
		assertThat(limits.tryAcquire("maria@example.com")).isFalse();
		assertThat(limits.tryAcquire("joao@example.com")).isTrue();

		clock.advance(Duration.ofMinutes(61));

		assertThat(limits.tryAcquire("maria@example.com")).isTrue();
	}

	@Test
	void releaseGivesTheSlotBack() {
		var limits = new InboundLimits(1, 1, clock);
		assertThat(limits.tryAcquire("a@example.com")).isTrue();
		assertThat(limits.tryAcquire("a@example.com")).isFalse();

		limits.release("a@example.com");

		assertThat(limits.tryAcquire("a@example.com")).isTrue();
	}

	@Test
	void limitsTheDay() {
		var limits = new InboundLimits(5, 3, clock);
		assertThat(limits.tryAcquire("a@example.com")).isTrue();
		assertThat(limits.tryAcquire("b@example.com")).isTrue();
		assertThat(limits.tryAcquire("c@example.com")).isTrue();
		assertThat(limits.tryAcquire("d@example.com")).isFalse();

		clock.set(Instant.parse("2026-11-13T00:00:01Z"));

		assertThat(limits.tryAcquire("d@example.com")).isTrue();
	}

	@Test
	void remembersEventsFor24Hours() {
		var limits = new InboundLimits(5, 80, clock);
		assertThat(limits.seen("e1")).isFalse();
		limits.remember("e1");
		assertThat(limits.seen("e1")).isTrue();

		clock.advance(Duration.ofHours(24).plusSeconds(1));

		assertThat(limits.seen("e1")).isFalse();
	}

	@Test
	void extractsTheAddress() {
		assertThat(InboundLimits.address("  Maria Silva <MARIA@Example.com> ")).isEqualTo("maria@example.com");
		assertThat(InboundLimits.address(" Joao@Example.com ")).isEqualTo("joao@example.com");
	}

	// The per-sender key is an HMAC-SHA256 of the address under a random salt
	// generated in the constructor, so no field may hold the plain address.
	@Test
	void keepsNoPlainAddress() throws Exception {
		var limits = new InboundLimits(5, 80, clock);
		limits.tryAcquire("Maria <maria@example.com>");

		for (Field field : InboundLimits.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			field.setAccessible(true);
			var value = field.get(limits);
			if (value instanceof String text) {
				assertThat(text).doesNotContain("maria@example.com");
			}
			if (value instanceof Map<?, ?> map) {
				assertThat(map.keySet().toString()).doesNotContain("maria@example.com");
				assertThat(map.values().toString()).doesNotContain("maria@example.com");
			}
			if (value instanceof Collection<?> collection) {
				assertThat(collection.toString()).doesNotContain("maria@example.com");
			}
		}
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		void advance(Duration duration) {
			now = now.plus(duration);
		}

		void set(Instant instant) {
			now = instant;
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}

	}

}
