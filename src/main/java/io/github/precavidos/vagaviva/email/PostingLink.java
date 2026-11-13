package io.github.precavidos.vagaviva.email;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Carries a job posting inside a link: the UTF-8 text is raw-deflated and
 * written as base64url without padding, after {@code #vaga=}. The fragment
 * never reaches the server, and the browser decodes it with
 * {@code DecompressionStream('deflate-raw')}.
 */
final class PostingLink {

	static final int MAX_LENGTH = 8_000;

	private static final String FRAGMENT = "/#vaga=";

	private PostingLink() {
	}

	static Optional<String> of(String publicUrl, String postingText) {
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
		try {
			ByteArrayOutputStream compressed = new ByteArrayOutputStream();
			try (DeflaterOutputStream out = new DeflaterOutputStream(compressed, deflater)) {
				out.write(postingText.getBytes(UTF_8));
			}
			String link = publicUrl + FRAGMENT
					+ Base64.getUrlEncoder().withoutPadding().encodeToString(compressed.toByteArray());
			return link.length() <= MAX_LENGTH ? Optional.of(link) : Optional.empty();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		finally {
			deflater.end();
		}
	}

	static String decode(String fragmentValue) {
		byte[] compressed = Base64.getUrlDecoder().decode(fragmentValue);
		Inflater inflater = new Inflater(true);
		try (InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(compressed), inflater)) {
			return new String(in.readAllBytes(), UTF_8);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Not a deflated posting", e);
		}
		finally {
			inflater.end();
		}
	}

}
