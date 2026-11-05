package io.github.precavidos.vagaviva.vocabulario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class VocabularioDeCompetenciasTests {

	private static final String ESCO_JAVA = """
			{ "uri": "http://data.europa.eu/esco/skill/19a8293b-8e95-4de3-983f-77484079c389",
			  "rotuloPt": "Java (programação informática)", "rotuloEn": "Java (computer programming)" }""";

	@Test
	void arquivoDoArquetipoJavaSpringEhValido() {
		var vocabulario = VocabularioDeCompetencias.doClasspath("vocabulario/java-spring.json");

		assertThat(vocabulario.arquetipo()).isEqualTo("java-spring");
		assertThat(vocabulario.competencia("java")).hasValueSatisfying(java -> {
			assertThat(java.origem()).isEqualTo(Competencia.Origem.ESCO);
			assertThat(java.esco().uri()).startsWith("http://data.europa.eu/esco/skill/");
		});
		assertThat(vocabulario.competencia("spring"))
			.hasValueSatisfying(spring -> assertThat(spring.origem()).isEqualTo(Competencia.Origem.PROPRIA));
		assertThat(vocabulario.tecnologias()).extracting(TecnologiaNomeada::id).contains("java", "spring-boot");
	}

	@Test
	void vocabularioMinimoValido() {
		var vocabulario = ler("""
				{ "arquetipo": "teste",
				  "competencias": [ { "id": "java", "rotulo": "Java", "esco": %s, "termos": [] } ],
				  "tecnologias": [ { "id": "java", "nome": "Java", "termos": ["Java"], "competencias": ["java"] } ],
				  "termosRecusados": [] }""".formatted(ESCO_JAVA));

		assertThat(vocabulario.competencias()).hasSize(1);
		assertThat(vocabulario.competencia("inexistente")).isEmpty();
	}

	@Test
	void recusaIdDeCompetenciaRepetido() {
		assertInvalido("""
				{ "arquetipo": "teste",
				  "competencias": [
				    { "id": "java", "rotulo": "Java", "termos": [] },
				    { "id": "java", "rotulo": "Java de novo", "termos": [] } ],
				  "tecnologias": [], "termosRecusados": [] }""", "competência repetida: java");
	}

	@Test
	void recusaTecnologiaQueApontaParaCompetenciaInexistente() {
		assertInvalido("""
				{ "arquetipo": "teste",
				  "competencias": [ { "id": "java", "rotulo": "Java", "termos": [] } ],
				  "tecnologias": [ { "id": "kotlin", "nome": "Kotlin", "termos": ["Kotlin"], "competencias": ["kotlin"] } ],
				  "termosRecusados": [] }""", "tecnologia kotlin aponta para competência inexistente: kotlin");
	}

	@Test
	void recusaTecnologiaSemCompetencias() {
		assertInvalido("""
				{ "arquetipo": "teste",
				  "competencias": [],
				  "tecnologias": [ { "id": "java", "nome": "Java", "termos": ["Java"], "competencias": [] } ],
				  "termosRecusados": [] }""", "tecnologia java não aponta para nenhuma competência");
	}

	@Test
	void recusaTecnologiaSemTermos() {
		assertInvalido("""
				{ "arquetipo": "teste",
				  "competencias": [ { "id": "java", "rotulo": "Java", "termos": [] } ],
				  "tecnologias": [ { "id": "java", "nome": "Java", "termos": [], "competencias": ["java"] } ],
				  "termosRecusados": [] }""", "tecnologia java não tem termos");
	}

	@Test
	void recusaMesmoTermoEmDuasEntradasIgnorandoCaixaEAcentos() {
		assertInvalido("""
				{ "arquetipo": "teste",
				  "competencias": [
				    { "id": "testes", "rotulo": "Testes", "termos": ["Testes unitários"] },
				    { "id": "outra", "rotulo": "Outra", "termos": [] } ],
				  "tecnologias": [ { "id": "junit", "nome": "JUnit", "termos": ["testes  unitarios"], "competencias": ["outra"] } ],
				  "termosRecusados": [] }""", "termo \"testes  unitarios\" aparece em mais de uma entrada");
	}

	@Test
	void recusaTermoDaListaDeRecusados() {
		assertInvalido("""
				{ "arquetipo": "teste",
				  "competencias": [ { "id": "spring", "rotulo": "Spring", "termos": [] } ],
				  "tecnologias": [ { "id": "spring", "nome": "Spring", "termos": ["spring"], "competencias": ["spring"] } ],
				  "termosRecusados": [ { "termo": "Spring", "motivo": "estação do ano" } ] }""",
				"termo recusado em uso: spring (estação do ano)");
	}

	@Test
	void recusaUriQueNaoEhDeCompetenciaEsco() {
		assertInvalido("""
				{ "arquetipo": "teste",
				  "competencias": [ { "id": "java", "rotulo": "Java",
				    "esco": { "uri": "http://data.europa.eu/esco/occupation/123", "rotuloPt": "x", "rotuloEn": "x" },
				    "termos": [] } ],
				  "tecnologias": [], "termosRecusados": [] }""", "competência java tem URI ESCO inválida");
	}

	@Test
	void recusaRotuloEmBranco() {
		assertInvalido("""
				{ "arquetipo": "teste",
				  "competencias": [ { "id": "java", "rotulo": " ", "termos": [] } ],
				  "tecnologias": [], "termosRecusados": [] }""", "competência java sem rótulo");
	}

	@Test
	void listaTodosOsProblemasDeUmaVez() {
		assertThatThrownBy(() -> ler("""
				{ "arquetipo": "teste",
				  "competencias": [ { "id": "java", "rotulo": "", "termos": [] } ],
				  "tecnologias": [ { "id": "go", "nome": "Go", "termos": [], "competencias": ["go"] } ],
				  "termosRecusados": [] }"""))
			.isInstanceOf(VocabularioInvalidoException.class)
			.satisfies(e -> assertThat(((VocabularioInvalidoException) e).problemas()).hasSize(3));
	}

	private static void assertInvalido(String json, String problema) {
		assertThatThrownBy(() -> ler(json)).isInstanceOf(VocabularioInvalidoException.class)
			.satisfies(e -> assertThat(((VocabularioInvalidoException) e).problemas()).contains(problema));
	}

	private static VocabularioDeCompetencias ler(String json) {
		InputStream entrada = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
		return VocabularioDeCompetencias.ler(entrada);
	}

}
