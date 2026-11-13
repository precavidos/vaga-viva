package io.github.precavidos.vagaviva.email;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SvixSignatureTests {

	static final String SECRET = "whsec_MfKQ9r8GKYqrTwjUPD8ILPZIo2LaLaSw";
	static final String ID = "msg_p5jXN8AQM9LWM0D4loKWxJek";
	static final String TS = "1614265330";
	static final byte[] BODY = "{\"test\": 2432232314}".getBytes(UTF_8);
	static final String SIG = "v1,g0hM9SsE+OTPJTGt/tmIKtSyZlE3uFJELVlNIOLJ1OE=";
	static final Instant AT = Instant.ofEpochSecond(1614265330);

	@Test
	void acceptsTheSvixDocumentationVector() {
		assertThat(SvixSignature.isValid(SECRET, ID, TS, BODY, SIG, AT)).isTrue();
	}

	@Test
	void acceptsWhenOneOfSeveralSignaturesMatches() {
		String header = "v1,AAAA v1,g0hM9SsE+OTPJTGt/tmIKtSyZlE3uFJELVlNIOLJ1OE=";
		assertThat(SvixSignature.isValid(SECRET, ID, TS, BODY, header, AT)).isTrue();
	}

	@Test
	void rejectsAChangedBody() {
		byte[] changed = "{\"test\": 2432232315}".getBytes(UTF_8);
		assertThat(SvixSignature.isValid(SECRET, ID, TS, changed, SIG, AT)).isFalse();
	}

	@Test
	void rejectsOutsideTheTolerance() {
		assertThat(SvixSignature.isValid(SECRET, ID, TS, BODY, SIG, AT.plusSeconds(301))).isFalse();
		assertThat(SvixSignature.isValid(SECRET, ID, TS, BODY, SIG, AT.minusSeconds(301))).isFalse();
		assertThat(SvixSignature.isValid(SECRET, ID, TS, BODY, SIG, AT.plusSeconds(299))).isTrue();
	}

	@Test
	void rejectsMalformedInput() {
		assertThat(SvixSignature.isValid(SECRET, ID, TS, BODY, null, AT)).isFalse();
		assertThat(SvixSignature.isValid(SECRET, ID, TS, BODY, "", AT)).isFalse();
		assertThat(SvixSignature.isValid(SECRET, ID, TS, BODY, "v2,g0hM9SsE+OTPJTGt/tmIKtSyZlE3uFJELVlNIOLJ1OE=", AT)).isFalse();
		assertThat(SvixSignature.isValid(SECRET, ID, TS, BODY, "v1,not-base64!", AT)).isFalse();
		assertThat(SvixSignature.isValid(SECRET, ID, "abc", BODY, SIG, AT)).isFalse();
		assertThat(SvixSignature.isValid("", ID, TS, BODY, SIG, AT)).isFalse();
	}

}
