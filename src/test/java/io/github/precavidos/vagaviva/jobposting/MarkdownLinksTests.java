package io.github.precavidos.vagaviva.jobposting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.jobposting.ListingPage.PageLink;

class MarkdownLinksTests {

	@Test
	void readsEveryLinkWithItsText() {
		var links = MarkdownLinks.parse("""
				# Vagas

				- [Pessoa Desenvolvedora Java](https://compass.gupy.io/jobs/1)
				- [Analista de Dados **Sênior**](https://compass.gupy.io/jobs/2 "título")
				""");

		assertThat(links).containsExactly(new PageLink("https://compass.gupy.io/jobs/1", "Pessoa Desenvolvedora Java"),
				new PageLink("https://compass.gupy.io/jobs/2", "Analista de Dados Sênior"));
	}

	@Test
	void keepsTheFirstLineOfACardAndDropsImagesInside() {
		var links = MarkdownLinks.parse(
				"[![logo](https://cdn.test/logo.png)\n\nDesenvolvedor Back-end\n\nRemoto\n\nEfetivo](https://compass.gupy.io/jobs/3?jobBoardSource=gupy_public_page)");

		assertThat(links).containsExactly(new PageLink(
				"https://compass.gupy.io/jobs/3?jobBoardSource=gupy_public_page", "Desenvolvedor Back-end"));
	}

	@Test
	void skipsALabelBeforeTheTitle() {
		var links = MarkdownLinks.parse(
				"[Vaga para Efetivo:\n\nAI Engineer | Senior (Hybrid/SP)\n\nTrabalho Remoto](/jobs/12945127?jobBoardSource=gupy_public_page)");

		assertThat(links).containsExactly(
				new PageLink("/jobs/12945127?jobBoardSource=gupy_public_page", "AI Engineer | Senior (Hybrid/SP)"));
	}

	@Test
	void keepsALinkThatAppearsTwice() {
		var links = MarkdownLinks.parse("[Apply](https://jobs.lever.co/palantir/1) [Software Engineer](https://jobs.lever.co/palantir/1)");

		assertThat(links).extracting(PageLink::text).containsExactly("Apply", "Software Engineer");
	}

	@Test
	void ignoresImagesAndTextWithoutLinks() {
		assertThat(MarkdownLinks.parse("![foto](https://cdn.test/a.png) e [colchetes] soltos (sem link)")).isEmpty();
	}

	@Test
	void cutsVeryLongTexts() {
		var links = MarkdownLinks.parse("[" + "a".repeat(400) + "](https://x.test/1)");

		assertThat(links.get(0).text()).hasSize(MarkdownLinks.MAX_TEXT_LENGTH);
	}

}
