package io.github.precavidos.vagaviva.vaga;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ExtracaoDeVagaApiTests {

	@Autowired
	MockMvcTester mvc;

	@Test
	void extraiNivelTecnologiasECompetencias() {
		var resposta = mvc.post()
			.uri("/api/vagas/extracao")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "texto": "Desenvolvedor Java Júnior. Requisitos: Spring Boot, PostgreSQL e testes unitários." }""");

		assertThat(resposta).hasStatusOk();
		var json = assertThat(resposta).bodyJson();
		json.extractingPath("$.arquetipo").isEqualTo("java-spring");
		json.extractingPath("$.niveis").asArray().containsExactly("JUNIOR");
		json.extractingPath("$.tecnologias[*].id").asArray().containsExactly("java", "spring-boot", "postgresql");
		json.extractingPath("$.tecnologias[1].termosEncontrados").asArray().containsExactly("Spring Boot");
		json.extractingPath("$.competencias[*].id")
			.asArray()
			.containsExactlyInAnyOrder("java", "spring", "postgresql", "sql", "testes-automatizados");
		json.extractingPath("$.competencias[?(@.id == 'java')].origem").asArray().containsExactly("ESCO");
		json.extractingPath("$.competencias[?(@.id == 'java')].escoUri")
			.asArray()
			.containsExactly("http://data.europa.eu/esco/skill/19a8293b-8e95-4de3-983f-77484079c389");
		json.extractingPath("$.competencias[?(@.id == 'spring')].origem").asArray().containsExactly("PROPRIA");
	}

	@Test
	void vagaSemNadaReconhecidoDevolveListasVazias() {
		var resposta = mvc.post()
			.uri("/api/vagas/extracao")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "texto": "Vaga para confeiteiro com experiência em bolos." }""");

		assertThat(resposta).hasStatusOk();
		assertThat(resposta).bodyJson().extractingPath("$.niveis").asArray().isEmpty();
		assertThat(resposta).bodyJson().extractingPath("$.tecnologias").asArray().isEmpty();
		assertThat(resposta).bodyJson().extractingPath("$.competencias").asArray().isEmpty();
	}

	@Test
	void recusaTextoEmBranco() {
		var resposta = mvc.post()
			.uri("/api/vagas/extracao")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "texto": "   " }""");

		assertThat(resposta).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void recusaTextoLongoDemaisSemEcoarNemRegistrarOTexto(CapturedOutput log) {
		String texto = "Vaga sigilosa da Empresa X, falar com Maria " + "a".repeat(ExtratorDeVaga.TAMANHO_MAXIMO);
		var resposta = mvc.post()
			.uri("/api/vagas/extracao")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"texto\": \"" + texto + "\" }");

		assertThat(resposta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(resposta).bodyText().doesNotContain("Empresa X");
		assertThat(log.getAll()).doesNotContain("Empresa X");
	}

	@Test
	void recusaJsonMalFormadoSemEcoarNemRegistrarOTexto(CapturedOutput log) {
		var resposta = mvc.post()
			.uri("/api/vagas/extracao")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"texto\": \"Vaga sigilosa da Empresa X\" ");

		assertThat(resposta).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(resposta).bodyText().doesNotContain("Empresa X");
		assertThat(log.getAll()).doesNotContain("Empresa X");
	}

}
