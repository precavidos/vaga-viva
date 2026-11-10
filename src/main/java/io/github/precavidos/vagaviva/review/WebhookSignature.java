package io.github.precavidos.vagaviva.review;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Checks GitHub's {@code X-Hub-Signature-256} header: an HMAC-SHA256 of the raw body with the webhook secret. */
final class WebhookSignature {

	private static final String PREFIX = "sha256=";

	private WebhookSignature() {
	}

	static boolean isValid(String secret, byte[] body, String header) {
		if (secret == null || secret.isBlank() || header == null || !header.startsWith(PREFIX)) {
			return false;
		}
		byte[] expected = hmac(secret, body);
		byte[] actual;
		try {
			actual = HexFormat.of().parseHex(header.substring(PREFIX.length()));
		}
		catch (IllegalArgumentException e) {
			return false;
		}
		return MessageDigest.isEqual(expected, actual);
	}

	static String sign(String secret, byte[] body) {
		return PREFIX + HexFormat.of().formatHex(hmac(secret, body));
	}

	private static byte[] hmac(String secret, byte[] body) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return mac.doFinal(body);
		}
		catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new IllegalStateException("HmacSHA256 unavailable", e);
		}
	}

}
