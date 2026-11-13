package io.github.precavidos.vagaviva.email;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * In-memory limits for incoming e-mails: how many replies each sender gets per
 * hour, how many the service sends per day, and which webhook events were
 * already handled. Nothing is persisted, and no plain e-mail address is kept:
 * the per-sender key is an HMAC-SHA256 of the normalized address under a random
 * salt generated at construction (ADR 0003).
 */
final class InboundLimits {

	private static final Duration HOUR = Duration.ofHours(1);

	private static final Duration EVENT_MEMORY = Duration.ofHours(24);

	private static final int MAX_EVENTS = 1_000;

	private final int perSenderPerHour;

	private final int perDay;

	private final Clock clock;

	private final byte[] salt = new byte[32];

	private final Map<String, Deque<Instant>> senders = new HashMap<>();

	private final Map<String, Instant> events = new LinkedHashMap<>() {

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Instant> eldest) {
			return size() > MAX_EVENTS;
		}

	};

	private LocalDate day;

	private int dayCount;

	InboundLimits(int perSenderPerHour, int perDay, Clock clock) {
		this.perSenderPerHour = perSenderPerHour;
		this.perDay = perDay;
		this.clock = clock;
		new SecureRandom().nextBytes(salt);
	}

	/** Whether the event id was remembered in the last 24 hours. */
	synchronized boolean seen(String eventId) {
		var at = events.get(eventId);
		if (at == null) {
			return false;
		}
		if (at.plus(EVENT_MEMORY).isBefore(clock.instant())) {
			events.remove(eventId);
			return false;
		}
		return true;
	}

	synchronized void remember(String eventId) {
		events.remove(eventId);
		events.put(eventId, clock.instant());
	}

	/** Takes one reply from the sender's and the day's quota; false, taking nothing, when either is spent. */
	synchronized boolean tryAcquire(String sender) {
		var now = clock.instant();
		var today = now.atOffset(ZoneOffset.UTC).toLocalDate();
		if (!today.equals(day)) {
			day = today;
			dayCount = 0;
		}
		if (dayCount >= perDay) {
			return false;
		}
		var recent = senders.computeIfAbsent(key(sender), k -> new ArrayDeque<>());
		var cutoff = now.minus(HOUR);
		while (!recent.isEmpty() && !recent.peekFirst().isAfter(cutoff)) {
			recent.removeFirst();
		}
		if (recent.size() >= perSenderPerHour) {
			return false;
		}
		recent.addLast(now);
		dayCount++;
		senders.values().removeIf(Deque::isEmpty);
		return true;
	}

	/** The address inside {@code Name <a@b>}, or the whole text, lower-cased and trimmed. */
	static String address(String sender) {
		var text = sender == null ? "" : sender;
		var open = text.lastIndexOf('<');
		var close = text.lastIndexOf('>');
		if (open >= 0 && close > open) {
			text = text.substring(open + 1, close);
		}
		return text.strip().toLowerCase(Locale.ROOT);
	}

	private String key(String sender) {
		try {
			var mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(salt, "HmacSHA256"));
			return Base64.getEncoder().encodeToString(mac.doFinal(address(sender).getBytes(UTF_8)));
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("HmacSHA256 is unavailable");
		}
	}

}
