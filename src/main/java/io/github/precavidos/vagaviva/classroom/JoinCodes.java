package io.github.precavidos.vagaviva.classroom;

import java.security.SecureRandom;

/** Makes the codes students use to join a classroom. */
final class JoinCodes {

	static final int LENGTH = 10;

	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

	private static final SecureRandom RANDOM = new SecureRandom();

	private JoinCodes() {
	}

	/** Ten characters from {@code [a-z0-9]}. */
	static String next() {
		var code = new StringBuilder(LENGTH);
		for (int i = 0; i < LENGTH; i++) {
			code.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
		}
		return code.toString();
	}

}
