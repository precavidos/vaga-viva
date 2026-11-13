package io.github.precavidos.vagaviva.email;

import java.util.Map;

/** An e-mail to send through Resend; {@code idempotencyKey} makes a retry harmless. */
public record OutgoingEmail(String to, String subject, String text, String html, Map<String, String> headers,
		String idempotencyKey) {

	@Override
	public String toString() {
		return "OutgoingEmail[<redacted>]";
	}

}
