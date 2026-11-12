package io.github.precavidos.vagaviva.email;

import java.util.Map;

/**
 * An e-mail Resend received for us. Missing fields are empty strings; header
 * names are lower-cased. Lives in memory only (ADR 0003).
 */
public record ReceivedEmail(String from, String subject, String messageId, String text, String html,
		Map<String, String> headers) {

	@Override
	public String toString() {
		return "ReceivedEmail[<redacted>]";
	}

}
