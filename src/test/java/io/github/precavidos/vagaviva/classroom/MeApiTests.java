package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

/** "/api/me" tells the top bar who is signed in and whether they own a classroom. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "vagaviva.classroom.teachers=prof")
class MeApiTests {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	ClientRegistrationRepository registrations;

	@Autowired
	ClassroomService classrooms;

	@Test
	void nobodyIsSignedIn() {
		assertThat(mvc.get().uri("/api/me")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void aStudentDoesNotTeach() {
		var json = assertThat(me("maria")).hasStatusOk().bodyJson();
		json.extractingPath("$.login").isEqualTo("maria");
		json.extractingPath("$.teaches").isEqualTo(false);
	}

	/** Off by default, so the bar does not link to a page that answers 404. */
	@Test
	void saysTheCompanyWatchIsOff() {
		assertThat(me("maria")).hasStatusOk().bodyJson().extractingPath("$.companyWatch").isEqualTo(false);
	}

	@Test
	void theOwnerOfAClassroomTeaches() {
		classrooms.create("prof", "Desenvolvedora Java Júnior com Spring Boot, PostgreSQL e Docker.", "Turma", null,
				false);

		var json = assertThat(me("prof")).hasStatusOk().bodyJson();
		json.extractingPath("$.login").isEqualTo("prof");
		json.extractingPath("$.teaches").isEqualTo(true);
	}

	private MvcTestResult me(String login) {
		return mvc.get()
			.uri("/api/me")
			.with(oauth2Login().clientRegistration(registrations.findByRegistrationId("github"))
				.attributes((attributes) -> {
					attributes.put("id", 42);
					attributes.put("login", login);
				}))
			.exchange();
	}

}
