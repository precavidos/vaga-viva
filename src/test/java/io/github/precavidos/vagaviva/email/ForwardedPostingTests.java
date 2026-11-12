package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import io.github.precavidos.vagaviva.jobposting.JobPostingExtractor;

class ForwardedPostingTests {

	@ParameterizedTest
	@ValueSource(strings = { "gmail-pt.txt", "gmail-en.txt", "outlook-pt.txt", "apple-mail.txt", "with-signature.txt" })
	void startsAtTheFirstLineOfThePosting(String fixture) throws IOException {
		String result = ForwardedPosting.text(email(fixture(fixture), ""));

		assertThat(result).startsWith("Desenvolvedora Java Júnior");
		assertThat(result).contains("Spring Boot");
		assertThat(result).doesNotContain("olha essa vaga", "Forwarded message", "Mensagem encaminhada",
				"Begin forwarded message", "De:", "From:", "Assunto:", "Subject:");
	}

	@Test
	void usesTheHtmlWhenTextIsBlank() throws IOException {
		String result = ForwardedPosting.text(email("", fixture("html-only.html")));

		assertThat(result).startsWith("Desenvolvedora Java Júnior\nEmpresa Exemplo Tecnologia Ltda. - Remoto\n");
		assertThat(result).contains("Requisitos\n- Conhecimento de Java e Spring Boot\n- Noções de SQL");
		assertThat(result).doesNotContain("olha essa vaga", "Mensagem encaminhada", "De:", "Assunto:", "<");
	}

	@Test
	void keepsTheWholeBodyWithoutAForwardLine() throws IOException {
		String body = fixture("no-forward-line.txt");

		assertThat(ForwardedPosting.text(email(body, ""))).isEqualTo(body.strip());
	}

	@Test
	void dropsTheSignature() throws IOException {
		String result = ForwardedPosting.text(email(fixture("with-signature.txt"), ""));

		assertThat(result).doesNotContain("Enviado do meu celular", "aluna@example.com");
		assertThat(result).endsWith("horário flexível.");
	}

	@Test
	void capsAtTheExtractorLimit() {
		String text = "a".repeat(JobPostingExtractor.MAX_LENGTH + 500);

		assertThat(ForwardedPosting.text(email(text, ""))).hasSize(JobPostingExtractor.MAX_LENGTH);
	}

	@Test
	void blankEmailGivesBlankText() {
		assertThat(ForwardedPosting.text(email("  ", " \n"))).isEmpty();
	}

	private static ReceivedEmail email(String text, String html) {
		return new ReceivedEmail("", "", "", text, html, Map.of());
	}

	private static String fixture(String name) throws IOException {
		return new ClassPathResource("email/" + name).getContentAsString(StandardCharsets.UTF_8);
	}

}
