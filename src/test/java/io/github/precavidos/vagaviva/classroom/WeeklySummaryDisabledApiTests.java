package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

/** Without a secret in the configuration, the route does not exist. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "vagaviva.classroom.summary-secret=")
@AutoConfigureMockMvc
class WeeklySummaryDisabledApiTests {

	@Autowired
	MockMvcTester mvc;

	@Test
	void blankSecretTurnsTheRouteOff() {
		var response = mvc.post().uri("/api/classrooms/weekly-summaries").header("X-Vaga-Viva-Secret", "").exchange();

		assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
	}

}
