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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "vagaviva.classroom.teachers=david")
class ClassroomApiTests {

	private static final String POSTING = "Desenvolvedora Java Júnior com Spring Boot, PostgreSQL e Docker. "
			+ "Contato: rh@empresa.example";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	ClientRegistrationRepository registrations;

	@Test
	void aTeacherCreatesAClassroom() {
		var response = create("david", "Turma de 2026", null, false);

		assertThat(response).hasStatus(HttpStatus.CREATED);
		var json = assertThat(response).bodyJson();
		json.extractingPath("$.name").isEqualTo("Turma de 2026");
		json.extractingPath("$.joinCode").asString().matches("[a-z0-9]{10}");
		json.extractingPath("$.id").asString().isNotBlank();
		String code = (String) json.extractingPath("$.joinCode").actual();
		json.extractingPath("$.inviteUrl").isEqualTo("/turma/" + code);
	}

	@Test
	void onlyPilotTeachersCreateClassrooms() {
		var response = create("maria", "Turma", null, false);

		assertThat(response).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(response).bodyJson()
			.extractingPath("$.detail")
			.isEqualTo("Só professores do piloto podem criar turmas.");
	}

	@Test
	void creatingRequiresSigningIn() {
		var response = mvc.post()
			.uri("/api/classrooms")
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body("Turma", null, false))
			.exchange();

		assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void rejectsAnEmailWithoutConsentAndBadNames() {
		assertThat(create("david", "Turma", "prof@escola.example", false)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(create("david", "   ", null, false)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(create("david", "x".repeat(81), null, false)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(create("david", "Turma", "prof@escola.example", true)).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void listsOnlyTheTeachersOwnClassrooms() {
		create("david", "Turma da lista", null, false);

		var mine = mvc.get().uri("/api/classrooms").with(login("david")).exchange();
		var others = mvc.get().uri("/api/classrooms").with(login("maria")).exchange();

		assertThat(mine).hasStatusOk();
		assertThat(mine).bodyJson().extractingPath("$[?(@.name=='Turma da lista')].students").asArray().containsExactly(0);
		assertThat(others).hasStatusOk();
		assertThat(others).bodyJson().extractingPath("$").asArray().isEmpty();
		assertThat(mvc.get().uri("/api/classrooms")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void theInvitePageNeedsNoLoginAndListsNoStudents() {
		String code = joinCodeOf(create("david", "Turma aberta", null, false));

		var response = mvc.get().uri("/api/classrooms/" + code).exchange();

		assertThat(response).hasStatusOk();
		var json = assertThat(response).bodyJson();
		json.extractingPath("$.name").isEqualTo("Turma aberta");
		json.extractingPath("$.teacherLogin").isEqualTo("david");
		json.extractingPath("$.archetypeName").asString().isNotBlank();
		json.extractingPath("$.missions[0].order").isEqualTo(1);
		json.extractingPath("$.missions[0].id").asString().isNotBlank();
		json.extractingPath("$.missions[0].title").asString().isNotBlank();
		assertThat(response).bodyText().doesNotContain("student").doesNotContain("rh@");
	}

	@Test
	void unknownJoinCodeIs404() {
		assertThat(mvc.get().uri("/api/classrooms/naoexiste01")).hasStatus(HttpStatus.NOT_FOUND);
	}

	private MvcTestResult create(String login, String name, String email,
			boolean consent) {
		return mvc.post()
			.uri("/api/classrooms")
			.with(login(login))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(name, email, consent))
			.exchange();
	}

	private String joinCodeOf(MvcTestResult response) {
		return (String) assertThat(response).bodyJson().extractingPath("$.joinCode").actual();
	}

	private static String body(String name, String email, boolean consent) {
		return """
				{ "text": "%s", "name": "%s", "teacherEmail": %s, "emailConsent": %s }""".formatted(POSTING, name,
				email == null ? "null" : "\"" + email + "\"", consent);
	}

	private OAuth2LoginRequestPostProcessor login(String login) {
		return oauth2Login().clientRegistration(registrations.findByRegistrationId("github"))
			.attributes((attributes) -> {
				attributes.put("id", 42);
				attributes.put("login", login);
			});
	}

}
