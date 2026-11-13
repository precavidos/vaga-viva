package io.github.precavidos.vagaviva.email;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Checks a Svix webhook signature (the scheme Resend uses): an HMAC-SHA256 of
 * {@code "<id>.<timestamp>.<raw body>"} keyed with the base64-decoded secret,
 * sent as a space-separated list of {@code v1,<base64>}.
 */
final class SvixSignature {

	private static final String SECRET_PREFIX = "whsec_";

	private static final String VERSION = "v1,";

	private static final Duration TOLERANCE = Duration.ofMinutes(5);

	private SvixSignature() {
	}

	static boolean isValid(String secret, String id, String timestamp, byte[] body, String header, Instant now) {
		if (secret == null || secret.isBlank() || id == null || timestamp == null || body == null || header == null
				|| now == null) {
			return false;
		}
		try {
			Instant sent = Instant.ofEpochSecond(Long.parseLong(timestamp));
			if (Duration.between(sent, now).abs().compareTo(TOLERANCE) > 0) {
				return false;
			}
			String encodedKey = secret.startsWith(SECRET_PREFIX) ? secret.substring(SECRET_PREFIX.length()) : secret;
			byte[] expected = hmac(Base64.getDecoder().decode(encodedKey), id, timestamp, body);
			boolean match = false;
			for (String candidate : header.split(" ")) {
				if (!candidate.startsWith(VERSION)) {
					continue;
				}
				byte[] actual;
				try {
					actual = Base64.getDecoder().decode(candidate.substring(VERSION.length()));
				}
				catch (IllegalArgumentException e) {
					continue;
				}
				match |= MessageDigest.isEqual(expected, actual);
			}
			return match;
		}
		catch (IllegalArgumentException | ArithmeticException | java.time.DateTimeException e) {
			return false;
		}
	}

	private static byte[] hmac(byte[] key, String id, String timestamp, byte[] body) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			mac.update((id + "." + timestamp + ".").getBytes(StandardCharsets.UTF_8));
			return mac.doFinal(body);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("HmacSHA256 unavailable", e);
		}
	}

}
