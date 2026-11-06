package io.github.precavidos.vagaviva.jobposting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class JobPostingPageReaderTests {

	private static final String REQUIREMENTS = "Requisitos: Java 21, Spring Boot, PostgreSQL, testes automatizados "
			+ "com JUnit, Git e APIs REST. O time cuida do agendamento de consultas da Clínica Exemplo e "
			+ "procura alguém com vontade de aprender, perguntar e revisar código com cuidado.";

	private final JobPostingPageReader reader = new JobPostingPageReader();

	@Test
	void readsTitleAndDescriptionFromJsonLd() {
		String text = reader.read(page("json-ld.html"));

		assertThat(text).startsWith("Desenvolvedor Java Júnior")
			.contains("Spring Boot")
			.doesNotContain("<p>")
			.doesNotContain("Entrar")
			.doesNotContain("cookies");
	}

	@Test
	void fallsBackToMainTextWithoutJsonLd() {
		String text = reader.read(page("no-json-ld.html"));

		assertThat(text).contains("Analista de Sistemas Júnior")
			.contains("Bancos relacionais, de preferência PostgreSQL.")
			.doesNotContain("Política de privacidade")
			.doesNotContain("Vagas parecidas")
			.doesNotContain("segredo")
			.doesNotContain("Receba novas vagas")
			.doesNotContain("Carreiras");
	}

	@Test
	void fallsBackToArticleThenBody() {
		String withArticle = reader.read(html("<body><nav>Menu do site</nav><div>Rodapé solto</div><article>"
				+ REQUIREMENTS + "</article></body>"));
		String bodyOnly = reader.read(html("<body><nav>Menu do site</nav><div>" + REQUIREMENTS + "</div></body>"));

		assertThat(withArticle).isEqualTo(REQUIREMENTS);
		assertThat(bodyOnly).isEqualTo(REQUIREMENTS);
	}

	@Test
	void spaShellIsUnreadable() {
		assertThatThrownBy(() -> reader.read(page("spa-shell.html"))).isInstanceOf(UnreadablePageException.class);
	}

	@Test
	void findsJobPostingInsideGraph() {
		String text = reader.read(html(jsonLd("""
				{ "@context": "https://schema.org", "@graph": [
				  { "@type": "Organization", "name": "Clínica Exemplo" },
				  { "@type": "JobPosting", "title": "Dev Júnior", "description": "%s" } ] }""".formatted(REQUIREMENTS))
				+ "<body><main>Texto da página que não deve ser usado.</main></body>"));

		assertThat(text).isEqualTo("Dev Júnior\n\n" + REQUIREMENTS);
	}

	@Test
	void findsJobPostingWithTypeArray() {
		String text = reader.read(html(jsonLd("""
				{ "@type": ["JobPosting"], "title": "Dev Júnior", "description": "%s" }""".formatted(REQUIREMENTS))));

		assertThat(text).isEqualTo("Dev Júnior\n\n" + REQUIREMENTS);
	}

	@Test
	void findsJobPostingInTopLevelArrayOrLaterScript() {
		String laterScript = reader.read(html(jsonLd("""
				{ "@type": "Organization", "name": "Clínica Exemplo" }""") + jsonLd("""
				{ "@type": "JobPosting", "title": "Dev Júnior", "description": "%s" }""".formatted(REQUIREMENTS))));
		String topLevelArray = reader.read(html(jsonLd("""
				[ { "@type": "WebSite" }, { "@type": "JobPosting", "title": "Dev Júnior", "description": "%s" } ]"""
			.formatted(REQUIREMENTS))));

		assertThat(laterScript).isEqualTo("Dev Júnior\n\n" + REQUIREMENTS);
		assertThat(topLevelArray).isEqualTo("Dev Júnior\n\n" + REQUIREMENTS);
	}

	@Test
	void malformedJsonLdFallsBackToPageText() {
		String text = reader.read(html(jsonLd("""
				{ "@type": "JobPosting", "title": "Dev Júnior", "description": "curta", }""")
				+ "<body><main>" + REQUIREMENTS + "</main></body>"));

		assertThat(text).isEqualTo(REQUIREMENTS);
	}

	@Test
	void shortJsonLdDescriptionFallsBackToPageText() {
		String text = reader.read(html(jsonLd("""
				{ "@type": "JobPosting", "title": "Dev", "description": "Vaga de dev júnior." }""")
				+ "<body><main>" + REQUIREMENTS + "</main></body>"));

		assertThat(text).isEqualTo(REQUIREMENTS);
	}

	@Test
	void unescapesDoubleEncodedDescription() {
		String text = reader.read(html(jsonLd("""
				{ "@type": "JobPosting", "title": "Dev Júnior", "description": "&lt;p&gt;%s&lt;/p&gt;" }"""
			.formatted(REQUIREMENTS))));

		assertThat(text).isEqualTo("Dev Júnior\n\n" + REQUIREMENTS);
	}

	@Test
	void decodesCharsetDeclaredOnlyInMeta() {
		byte[] latin1 = ("<html><head><meta charset=\"ISO-8859-1\"></head><body><main>Desenvolvedor Júnior. "
				+ REQUIREMENTS + "</main></body></html>")
			.getBytes(StandardCharsets.ISO_8859_1);

		assertThat(reader.read(new FetchedPage(latin1, null))).startsWith("Desenvolvedor Júnior.");
	}

	@Test
	void truncatesToTheExtractorLimit() {
		String text = reader.read(html("<body><main>" + "vaga ".repeat(6_000) + "</main></body>"));

		assertThat(text).hasSize(JobPostingExtractor.MAX_LENGTH);
	}

	private static String jsonLd(String json) {
		return "<script type=\"application/ld+json\">" + json + "</script>";
	}

	private static FetchedPage html(String html) {
		return new FetchedPage(html.getBytes(StandardCharsets.UTF_8), "UTF-8");
	}

	private static FetchedPage page(String fixture) {
		try (InputStream in = JobPostingPageReaderTests.class
			.getResourceAsStream("/jobposting/pages/" + fixture)) {
			return new FetchedPage(in.readAllBytes(), null);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
