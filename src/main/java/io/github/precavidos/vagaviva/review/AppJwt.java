package io.github.precavidos.vagaviva.review;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;

/**
 * Signs the short-lived JWT (RS256) that authenticates as the GitHub App
 * itself. Issued 60 seconds in the past to absorb clock drift and valid for
 * 9 minutes, under GitHub's 10-minute limit.
 */
final class AppJwt {

	private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();

	/** DER prefix that turns a PKCS#1 RSA key into PKCS#8: version 0 and the rsaEncryption algorithm id. */
	private static final byte[] RSA_ALGORITHM = { 0x02, 0x01, 0x00, 0x30, 0x0d, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48,
			(byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00 };

	private final String appId;

	private final PrivateKey key;

	private final Clock clock;

	AppJwt(String appId, String pem, Clock clock) {
		this.appId = appId;
		this.key = parse(pem);
		this.clock = clock;
	}

	String create() {
		Instant now = clock.instant();
		String header = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
		String payload = "{\"iat\":" + (now.getEpochSecond() - 60) + ",\"exp\":" + (now.getEpochSecond() + 540)
				+ ",\"iss\":\"" + appId.replace("\"", "") + "\"}";
		String signingInput = encode(header) + "." + encode(payload);
		try {
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initSign(key);
			signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
			return signingInput + "." + BASE64URL.encodeToString(signature.sign());
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException("Could not sign the GitHub App JWT", e);
		}
	}

	private static String encode(String json) {
		return BASE64URL.encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	/** Reads a PEM RSA key in PKCS#1 ({@code BEGIN RSA PRIVATE KEY}, GitHub's format) or PKCS#8. */
	static PrivateKey parse(String pem) {
		// Environment variables often carry the key on one line, with literal \n escapes.
		String text = pem.replace("\\n", "\n");
		boolean pkcs1 = text.contains("BEGIN RSA PRIVATE KEY");
		String base64 = text.replaceAll("-----(BEGIN|END) [A-Z ]+-----", "").replaceAll("\\s", "");
		byte[] der;
		try {
			der = Base64.getDecoder().decode(base64);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("The GitHub App private key is not valid PEM");
		}
		try {
			return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs1 ? toPkcs8(der) : der));
		}
		catch (GeneralSecurityException e) {
			throw new IllegalArgumentException("The GitHub App private key is not a valid RSA key");
		}
	}

	private static byte[] toPkcs8(byte[] pkcs1) {
		var privateKey = new ByteArrayOutputStream();
		privateKey.write(0x04);
		writeLength(privateKey, pkcs1.length);
		privateKey.writeBytes(pkcs1);
		var body = new ByteArrayOutputStream();
		body.writeBytes(RSA_ALGORITHM);
		body.writeBytes(privateKey.toByteArray());
		var sequence = new ByteArrayOutputStream();
		sequence.write(0x30);
		writeLength(sequence, body.size());
		sequence.writeBytes(body.toByteArray());
		return sequence.toByteArray();
	}

	private static void writeLength(ByteArrayOutputStream out, int length) {
		if (length < 0x80) {
			out.write(length);
		}
		else if (length < 0x100) {
			out.write(0x81);
			out.write(length);
		}
		else if (length < 0x10000) {
			out.write(0x82);
			out.write(length >> 8);
			out.write(length & 0xff);
		}
		else {
			out.write(0x83);
			out.write(length >> 16);
			out.write((length >> 8) & 0xff);
			out.write(length & 0xff);
		}
	}

}
