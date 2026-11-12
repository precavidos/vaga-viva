package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

/** The allow-list ignores the case of the list and of the login, and the spaces around the commas. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "vagaviva.classroom.teachers=David, HELENA, ")
class ClassroomTeachersCaseTests {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	ClientRegistrationRepository registrations;

	@Test
	void matchesWhateverTheCase() {
		assertThat(create("david")).hasStatus(HttpStatus.CREATED);
		assertThat(create("Helena")).hasStatus(HttpStatus.CREATED);
		assertThat(create("maria")).hasStatus(HttpStatus.FORBIDDEN);
	}

	private MvcTestResult create(String login) {
		return mvc.post()
			.uri("/api/classrooms")
			.with(oauth2Login().clientRegistration(registrations.findByRegistrationId("github"))
				.attributes((attributes) -> { attributes.put("id", 42); attributes.put("login", login); }))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"text\":\"Desenvolvedora Java Júnior com Spring Boot e PostgreSQL.\",\"name\":\"Turma\",\"emailConsent\":false}")
			.exchange();
	}

}
