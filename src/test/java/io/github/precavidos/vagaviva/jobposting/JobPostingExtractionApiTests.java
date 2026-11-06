package io.github.precavidos.vagaviva.jobposting;

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
class JobPostingExtractionApiTests {

	@Autowired
	MockMvcTester mvc;

	@Test
	void extractsSeniorityTechnologiesAndSkills() {
		var response = mvc.post()
			.uri("/api/job-postings/extraction")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "text": "Desenvolvedor Java Júnior. Requisitos: Spring Boot, PostgreSQL e testes unitários." }""");

		assertThat(response).hasStatusOk();
		var json = assertThat(response).bodyJson();
		json.extractingPath("$.archetype").isEqualTo("java-spring");
		json.extractingPath("$.archetypeFit.status").isEqualTo("COVERED");
		json.extractingPath("$.archetypeFit.uncovered").asArray().isEmpty();
		json.extractingPath("$.seniorityLevels").asArray().containsExactly("JUNIOR");
		json.extractingPath("$.technologies[*].id").asArray().containsExactly("java", "spring-boot", "postgresql");
		json.extractingPath("$.technologies[1].matchedTerms").asArray().containsExactly("Spring Boot");
		json.extractingPath("$.skills[*].id")
			.asArray()
			.containsExactlyInAnyOrder("java", "spring", "postgresql", "sql", "automated-testing");
		json.extractingPath("$.skills[?(@.id == 'java')].source").asArray().containsExactly("ESCO");
		json.extractingPath("$.skills[?(@.id == 'java')].escoUri")
			.asArray()
			.containsExactly("http://data.europa.eu/esco/skill/19a8293b-8e95-4de3-983f-77484079c389");
		json.extractingPath("$.skills[?(@.id == 'spring')].source").asArray().containsExactly("CUSTOM");
	}

	@Test
	void postingWithNothingRecognizedReturnsEmptyLists() {
		var response = mvc.post()
			.uri("/api/job-postings/extraction")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "text": "Vaga para confeiteiro com experiência em bolos." }""");

		assertThat(response).hasStatusOk();
		assertThat(response).bodyJson().extractingPath("$.seniorityLevels").asArray().isEmpty();
		assertThat(response).bodyJson().extractingPath("$.technologies").asArray().isEmpty();
		assertThat(response).bodyJson().extractingPath("$.skills").asArray().isEmpty();
	}

	@Test
	void rejectsBlankText() {
		var response = mvc.post()
			.uri("/api/job-postings/extraction")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "text": "   " }""");

		assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void rejectsTooLongTextWithoutEchoingOrLoggingIt(CapturedOutput log) {
		String text = "Vaga sigilosa da Empresa X, falar com Maria " + "a".repeat(JobPostingExtractor.MAX_LENGTH);
		var response = mvc.post()
			.uri("/api/job-postings/extraction")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"text\": \"" + text + "\" }");

		assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(response).bodyText().doesNotContain("Empresa X");
		assertThat(log.getAll()).doesNotContain("Empresa X");
	}

	@Test
	void rejectsMalformedJsonWithoutEchoingOrLoggingIt(CapturedOutput log) {
		var response = mvc.post()
			.uri("/api/job-postings/extraction")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"text\": \"Vaga sigilosa da Empresa X\" ");

		assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(response).bodyText().doesNotContain("Empresa X");
		assertThat(log.getAll()).doesNotContain("Empresa X");
	}

}
