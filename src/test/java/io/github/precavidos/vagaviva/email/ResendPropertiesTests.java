package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ResendPropertiesTests {

	private static ResendProperties of(String key, String secret, String from, String url) {
		return new ResendProperties(key, secret, from, url, 5, 80);
	}

	@Test
	void stripsTheTrailingSlashOfThePublicUrl() {
		assertThat(of("k", "s", "a@b.c", "https://vagaviva.dev/").publicUrl()).isEqualTo("https://vagaviva.dev");
	}

	@Test
	void neverShowsTheKeyOrTheSecret() {
		String text = of("re_key_123", "whsec_secret_456", "a@b.c", "https://vagaviva.dev").toString();
		assertThat(text).doesNotContain("re_key_123").doesNotContain("whsec_secret_456").contains("****");
	}

	@Test
	void inboundNeedsKeySecretFromAndUrl() {
		assertThat(of("k", "s", "a@b.c", "https://x.dev").inboundEnabled()).isTrue();
		assertThat(of("", "s", "a@b.c", "https://x.dev").inboundEnabled()).isFalse();
		assertThat(of("k", "", "a@b.c", "https://x.dev").inboundEnabled()).isFalse();
		assertThat(of("k", "s", "", "https://x.dev").inboundEnabled()).isFalse();
		assertThat(of("k", "s", "a@b.c", "").inboundEnabled()).isFalse();
		assertThat(of("k", "", "a@b.c", "https://x.dev").sendingEnabled()).isTrue();
	}

}
