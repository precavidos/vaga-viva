package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

class PostingLinkTests {

	static final String PUBLIC_URL = "https://vagaviva.dev";
	static final String MARKER = "#vaga=";

	@Test
	void roundTripsAccentsAndEmoji() {
		String posting = "🚀 Vaga Júnior — São Paulo\nRequisitos: Java, Spring";
		String link = PostingLink.of(PUBLIC_URL, posting).orElseThrow();
		assertThat(PostingLink.decode(fragment(link))).isEqualTo(posting);
	}

	@Test
	void startsWithThePublicUrlAndFragment() {
		String link = PostingLink.of(PUBLIC_URL, "x".repeat(300)).orElseThrow();
		assertThat(link).startsWith("https://vagaviva.dev/#vaga=");
	}

	@Test
	void usesBase64UrlWithoutPadding() {
		String link = PostingLink.of(PUBLIC_URL, "Java, Spring, Kafka ".repeat(20)).orElseThrow();
		assertThat(fragment(link)).matches("^[A-Za-z0-9_-]+$");
	}

	@Test
	void refusesLinksLongerThanTheLimit() {
		Random random = new Random(42);
		StringBuilder noise = new StringBuilder();
		for (int i = 0; i < 20_000; i++) {
			noise.append((char) ('!' + random.nextInt(94)));
		}
		assertThat(PostingLink.of(PUBLIC_URL, noise.toString())).isEmpty();
	}

	@Test
	void keepsLinksWithinTheLimit() {
		String posting = "Vaga Júnior em São Paulo, Java e Spring, sem experiência obrigatória. ".repeat(50);
		String link = PostingLink.of(PUBLIC_URL, posting).orElseThrow();
		assertThat(link.length()).isLessThanOrEqualTo(PostingLink.MAX_LENGTH);
	}

	static String fragment(String link) {
		return link.substring(link.indexOf(MARKER) + MARKER.length());
	}

}
