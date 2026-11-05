package io.github.precavidos.vagaviva.plan;

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
import io.github.precavidos.vagaviva.jobposting.JobPostingExtractor;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ProjectPlanApiTests {

	@Autowired
	MockMvcTester mvc;

	@Test
	void plansMissionsForAPosting() {
		var response = mvc.post()
			.uri("/api/project-plans")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "text": "Desenvolvedor Java Júnior com Spring Boot, PostgreSQL e Docker" }""");

		assertThat(response).hasStatusOk();
		var json = assertThat(response).bodyJson();
		json.extractingPath("$.archetype").isEqualTo("java-spring");
		json.extractingPath("$.domain.id").isEqualTo("clinic-scheduling");
		json.extractingPath("$.domain.name").isEqualTo("Agendamento de clínica");
		json.extractingPath("$.seniorityLevel").isEqualTo("JUNIOR");
		json.extractingPath("$.difficultyCap").isEqualTo(3);
		json.extractingPath("$.missions[*].id")
			.asArray()
			.containsExactly("patients-api", "persistence", "pagination", "scheduling-rules", "availability-query",
					"containerization");
		json.extractingPath("$.missions[*].order").asArray().containsExactly(1, 2, 3, 4, 5, 6);
		json.extractingPath("$.missions[0].title").isEqualTo("Cadastro de pacientes");
		json.extractingPath("$.missions[0].reason").isEqualTo("CORE");
		json.extractingPath("$.missions[4].reason").isEqualTo("SKILL_MATCH");
		json.extractingPath("$.missions[4].matchedSkills").asArray().containsExactly("sql", "postgresql");
	}

	@Test
	void rejectsTooLongTextWithoutEchoingOrLoggingIt(CapturedOutput log) {
		String text = "Vaga sigilosa da Empresa X, falar com Maria " + "a".repeat(JobPostingExtractor.MAX_LENGTH);
		var response = mvc.post()
			.uri("/api/project-plans")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"text\": \"" + text + "\" }");

		assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(response).bodyText().doesNotContain("Empresa X");
		assertThat(log.getAll()).doesNotContain("Empresa X");
	}

	@Test
	void rejectsMalformedJsonWithoutEchoingOrLoggingIt(CapturedOutput log) {
		var response = mvc.post()
			.uri("/api/project-plans")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"text\": \"Vaga sigilosa da Empresa X\" ");

		assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(response).bodyText().doesNotContain("Empresa X");
		assertThat(log.getAll()).doesNotContain("Empresa X");
	}

}
