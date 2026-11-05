package io.github.precavidos.vagaviva.vocabulario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReconhecimentoTests {

	private static final VocabularioDeCompetencias VOCABULARIO = VocabularioDeCompetencias
		.doClasspath("vocabulario/java-spring.json");

	@Test
	void reconheceTecnologiasECompetenciasQueElasEvidenciam() {
		var r = VOCABULARIO.reconhecer("Desenvolvedor Java com Spring Boot e PostgreSQL");

		assertThat(r.tecnologias()).extracting(t -> t.tecnologia().id())
			.containsExactly("java", "spring-boot", "postgresql");
		assertThat(r.competencias()).extracting(c -> c.competencia().id())
			.containsExactlyInAnyOrder("java", "spring", "postgresql", "sql");
	}

	@Test
	void javaNaoCasaComJavaScript() {
		var r = VOCABULARIO.reconhecer("Experiência com JavaScript, TypeScript e Javascript moderno");

		assertThat(r.tecnologias()).isEmpty();
		assertThat(r.competencias()).isEmpty();
	}

	@Test
	void casaPalavraInteiraSeparadaPorPontuacao() {
		var r = VOCABULARIO.reconhecer("Stack: Java/Spring, (Docker).");

		assertThat(r.tecnologias()).extracting(t -> t.tecnologia().id()).containsExactly("java", "docker");
	}

	@Test
	void termoRecusadoSozinhoNaoEhReconhecido() {
		var r = VOCABULARIO.reconhecer("Spring 2027 internship. The rest of the team has solid experience.");

		assertThat(r.tecnologias()).isEmpty();
		assertThat(r.competencias()).isEmpty();
	}

	@Test
	void ignoraCaixaAcentosEEspacosExtras() {
		var r = VOCABULARIO.reconhecer("SPRING   BOOT\ne testes unitarios");

		assertThat(r.tecnologias()).extracting(t -> t.tecnologia().id()).containsExactly("spring-boot");
		assertThat(r.competencias()).extracting(c -> c.competencia().id())
			.containsExactlyInAnyOrder("spring", "testes-automatizados");
	}

	@Test
	void sqlNaoCasaDentroDeNoSqlNemMySql() {
		var r = VOCABULARIO.reconhecer("Conhecimento em NoSQL e MySQL");

		assertThat(r.tecnologias()).isEmpty();
	}

	@Test
	void reconheceTermosComBarra() {
		var r = VOCABULARIO.reconhecer("Pipelines de CI/CD");

		assertThat(r.competencias()).extracting(c -> c.competencia().id()).containsExactly("integracao-continua");
	}

	@Test
	void guardaAsEvidenciasDeCadaCompetencia() {
		var r = VOCABULARIO.reconhecer("Usamos GitHub Actions e testes automatizados com JUnit");

		assertThat(r.tecnologias()).extracting(t -> t.tecnologia().id())
			.containsExactly("git", "junit", "ferramentas-de-ci");
		assertThat(r.competencias()).filteredOn(c -> c.competencia().id().equals("testes-automatizados"))
			.singleElement()
			.satisfies(c -> assertThat(c.evidencias()).containsExactly("testes automatizados", "JUnit"));
		assertThat(r.tecnologias()).filteredOn(t -> t.tecnologia().id().equals("git"))
			.singleElement()
			.satisfies(t -> assertThat(t.termosEncontrados()).containsExactly("GitHub"));
	}

	@Test
	void textoVazioNaoReconheceNada() {
		var r = VOCABULARIO.reconhecer("");

		assertThat(r.tecnologias()).isEmpty();
		assertThat(r.competencias()).isEmpty();
	}

}
