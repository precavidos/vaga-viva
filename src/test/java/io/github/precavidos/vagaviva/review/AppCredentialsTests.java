package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.InstallationToken;
import org.mockito.Mockito;

/** Webhook signatures, the app JWT and the installation token cache. */
class AppCredentialsTests {

	private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

	private static KeyPair keys;

	@BeforeAll
	static void generateKey() throws Exception {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		keys = generator.generateKeyPair();
	}

	@Test
	void acceptsOnlyTheSignatureOfTheExactBody() {
		byte[] body = "{\"action\":\"completed\"}".getBytes(StandardCharsets.UTF_8);
		String signature = WebhookSignature.sign("secret", body);

		assertThat(WebhookSignature.isValid("secret", body, signature)).isTrue();
		assertThat(WebhookSignature.isValid("other", body, signature)).isFalse();
		assertThat(WebhookSignature.isValid("secret", "{}".getBytes(StandardCharsets.UTF_8), signature)).isFalse();
		assertThat(WebhookSignature.isValid("secret", body, null)).isFalse();
		assertThat(WebhookSignature.isValid("secret", body, "sha1=abc")).isFalse();
		assertThat(WebhookSignature.isValid("secret", body, "sha256=not-hex")).isFalse();
		assertThat(WebhookSignature.isValid("", body, signature)).isFalse();
	}

	@Test
	void signsAnRs256JwtIssuedByTheApp() throws Exception {
		String jwt = new AppJwt("12345", pkcs8Pem(), Clock.fixed(NOW, ZoneOffset.UTC)).create();

		String[] parts = jwt.split("\\.");
		assertThat(parts).hasSize(3);
		assertThat(decode(parts[0])).contains("\"alg\":\"RS256\"");
		assertThat(decode(parts[1])).contains("\"iss\":\"12345\"")
			.contains("\"iat\":" + (NOW.getEpochSecond() - 60))
			.contains("\"exp\":" + (NOW.getEpochSecond() + 540));
		Signature verifier = Signature.getInstance("SHA256withRSA");
		verifier.initVerify(keys.getPublic());
		verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
		assertThat(verifier.verify(Base64.getUrlDecoder().decode(parts[2]))).isTrue();
	}

	@Test
	void readsTheKeyInGitHubsPkcs1FormatAndWithEscapedNewlines() {
		assertThat(AppJwt.parse(pkcs1Pem()).getEncoded()).isEqualTo(keys.getPrivate().getEncoded());
		assertThat(AppJwt.parse(pkcs1Pem().replace("\n", "\\n")).getEncoded())
			.isEqualTo(keys.getPrivate().getEncoded());
		assertThat(AppJwt.parse(pkcs8Pem()).getEncoded()).isEqualTo(keys.getPrivate().getEncoded());
	}

	@Test
	void rejectsAKeyThatIsNotPem() {
		assertThatThrownBy(() -> AppJwt.parse("not a key")).isInstanceOf(IllegalArgumentException.class)
			.hasMessageNotContaining("not a key");
	}

	@Test
	void reusesAnInstallationTokenUntilFiveMinutesBeforeItExpires() {
		GitHubApi github = Mockito.mock(GitHubApi.class);
		Mockito.when(github.createInstallationToken("jwt", 7L))
			.thenReturn(new InstallationToken("first", NOW.plus(Duration.ofHours(1))),
					new InstallationToken("second", NOW.plus(Duration.ofHours(2))));
		var clock = new MutableClock(NOW);
		var tokens = new InstallationTokens(github, () -> "jwt", clock);

		assertThat(tokens.forInstallation(7)).isEqualTo("first");
		clock.now = NOW.plus(Duration.ofMinutes(54));
		assertThat(tokens.forInstallation(7)).isEqualTo("first");
		clock.now = NOW.plus(Duration.ofMinutes(56));
		assertThat(tokens.forInstallation(7)).isEqualTo("second");
		Mockito.verify(github, Mockito.times(2)).createInstallationToken("jwt", 7L);
	}

	@Test
	void installationTokensNeverShowInToString() {
		assertThat(new InstallationToken("ghs_secret", NOW)).hasToString("InstallationToken[expiresAt=" + NOW + "]");
		assertThat(new GitHubAppProperties("1", "key", "secret", "vaga-viva").toString()).doesNotContain("key=key")
			.doesNotContain("secret]");
	}

	private static String decode(String part) {
		return new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8);
	}

	private static String pkcs8Pem() {
		return pem("PRIVATE KEY", keys.getPrivate().getEncoded());
	}

	/** GitHub hands out PKCS#1: the inner RSA key of the PKCS#8 structure. */
	private static String pkcs1Pem() {
		return pem("RSA PRIVATE KEY", innerOctetString(keys.getPrivate().getEncoded()));
	}

	private static String pem(String type, byte[] der) {
		return "-----BEGIN " + type + "-----\n" + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
			.encodeToString(der) + "\n-----END " + type + "-----\n";
	}

	/** The content of the last OCTET STRING of a PKCS#8 PrivateKeyInfo. */
	private static byte[] innerOctetString(byte[] pkcs8) {
		List<Integer> offsets = new ArrayList<>();
		int i = headerLength(pkcs8, 0);
		while (i < pkcs8.length) {
			offsets.add(i);
			i += headerLength(pkcs8, i) + contentLength(pkcs8, i);
		}
		int octetString = offsets.getLast();
		int start = octetString + headerLength(pkcs8, octetString);
		return Arrays.copyOfRange(pkcs8, start, start + contentLength(pkcs8, octetString));
	}

	private static int headerLength(byte[] der, int at) {
		int first = der[at + 1] & 0xff;
		return first < 0x80 ? 2 : 2 + (first & 0x7f);
	}

	private static int contentLength(byte[] der, int at) {
		int first = der[at + 1] & 0xff;
		if (first < 0x80) {
			return first;
		}
		int length = 0;
		for (int k = 0; k < (first & 0x7f); k++) {
			length = (length << 8) | (der[at + 2 + k] & 0xff);
		}
		return length;
	}

	static final class MutableClock extends Clock {

		Instant now;

		MutableClock(Instant now) {
			this.now = now;
		}

		@Override
		public Instant instant() {
			return now;
		}

		@Override
		public java.time.ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

	}

}
