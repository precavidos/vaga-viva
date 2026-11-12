package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;
import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import io.github.precavidos.vagaviva.provisioning.InstallAppProblem;
import io.github.precavidos.vagaviva.provisioning.ProvisionedRepository;
import io.github.precavidos.vagaviva.provisioning.StudentRepositoryProvisioner;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "vagaviva.classroom.teachers=david")
class EnrollmentApiTests {

	/** The access token {@code oauth2Login()} puts in the authorized client. */
	private static final String TOKEN = "access-token";

	private static final String POSTING = "Desenvolvedora Java Júnior com Spring Boot, PostgreSQL e Docker.";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	ClientRegistrationRepository registrations;

	@Autowired
	ClassroomService classrooms;

	@Autowired
	EnrollmentRepository enrollments;

	@Autowired
	EnrollmentProgressRepository progresses;

	@MockitoBean
	GitHubApi github;

	@MockitoBean
	StudentRepositoryProvisioner provisioner;

	@Test
	void aStudentJoinsWithNameAndConsent() {
		String code = newClassroom();
		given(github.isAppInstalledOn(TOKEN, "maria")).willReturn(true);
		given(provisioner.provision(eq(TOKEN), eq("maria"), any(ProjectPlan.class))).willReturn(repository("maria"));

		var response = join(code, "maria", "  Maria Souza  ", true);

		assertThat(response).hasStatus(HttpStatus.CREATED);
		var json = assertThat(response).bodyJson();
		json.extractingPath("$.fullName").isEqualTo("maria/vaga-viva-clinic");
		json.extractingPath("$.htmlUrl").isEqualTo("https://github.com/maria/vaga-viva-clinic");

		Classroom classroom = classrooms.byJoinCode(code).orElseThrow();
		StoredPlan stored = StoredPlan.fromJson(classroom.getPlan());
		var captor = ArgumentCaptor.forClass(ProjectPlan.class);
		verify(provisioner).provision(eq(TOKEN), eq("maria"), captor.capture());
		assertThat(captor.getValue().missions().stream().map((m) -> m.mission().id()).toList())
			.isEqualTo(stored.missionIds());

		Enrollment enrollment = enrollments.findByClassroomAndStudentLogin(classroom, "maria").orElseThrow();
		assertThat(enrollment.getDisplayName()).isEqualTo("Maria Souza");
		assertThat(enrollment.getRepositoryFullName()).isEqualTo("maria/vaga-viva-clinic");
		assertThat(enrollment.getConsentedAt()).isEqualTo(enrollment.getEnrolledAt());
		EnrollmentProgress progress = progresses.findById(enrollment.getId()).orElseThrow();
		assertThat(progress.getCurrentMissionId()).isEqualTo(stored.missionIds().get(0));
		assertThat(progress.getCurrentMissionSince()).isEqualTo(enrollment.getEnrolledAt());
		assertThat(progress.getLastActivityAt()).isEqualTo(enrollment.getEnrolledAt());
		assertThat(progress.isCompleted()).isFalse();
		assertThat(progress.getFailingTests()).isEqualTo("[]");
		assertThat(progress.getPullRequestsOnMission()).isEqualTo("[]");
		assertThat(progress.getHintsByMission()).isEqualTo("{}");
	}

	@Test
	void joiningTwiceReturnsTheSameRepository() {
		String code = newClassroom();
		given(github.isAppInstalledOn(TOKEN, "joana")).willReturn(true);
		given(provisioner.provision(eq(TOKEN), eq("joana"), any(ProjectPlan.class))).willReturn(repository("joana"));

		var first = join(code, "joana", "Joana", true);
		var second = join(code, "joana", "Joana de novo", true);

		assertThat(first).hasStatus(HttpStatus.CREATED);
		assertThat(second).hasStatusOk();
		assertThat(second).bodyJson().extractingPath("$.fullName").isEqualTo("joana/vaga-viva-clinic");
		verify(provisioner, times(1)).provision(eq(TOKEN), eq("joana"), any(ProjectPlan.class));
		Classroom classroom = classrooms.byJoinCode(code).orElseThrow();
		assertThat(enrollments.countByClassroom(classroom)).isEqualTo(1);
		assertThat(enrollments.findByClassroomAndStudentLogin(classroom, "joana").orElseThrow().getDisplayName())
			.isEqualTo("Joana");
	}

	@Test
	void consentIsRequired() {
		String code = newClassroom();

		var response = join(code, "carla", "Carla", false);

		assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(response).bodyJson().extractingPath("$.detail").asString().contains("aceite");
		verify(provisioner, never()).provision(any(), any(), any());
		assertThat(enrollments.countByClassroom(classrooms.byJoinCode(code).orElseThrow())).isZero();
	}

	@Test
	void nameIsRequiredAndAtMost40Characters() {
		String code = newClassroom();
		given(github.isAppInstalledOn(TOKEN, "paula")).willReturn(true);
		given(provisioner.provision(eq(TOKEN), eq("paula"), any(ProjectPlan.class))).willReturn(repository("paula"));

		for (String name : new String[] { "   ", "x".repeat(41) }) {
			var response = join(code, "paula", name, true);
			assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
			assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo("O nome deve ter de 1 a 40 caracteres.");
		}
		verify(provisioner, never()).provision(any(), any(), any());
		assertThat(join(code, "paula", "x".repeat(40), true)).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void asksToInstallTheAppFirst() {
		String code = newClassroom();
		given(github.isAppInstalledOn(TOKEN, "lia")).willReturn(false);

		var response = join(code, "lia", "Lia", true);

		assertThat(response).hasStatus(HttpStatus.CONFLICT);
		var json = assertThat(response).bodyJson();
		json.extractingPath("$.detail").isEqualTo(InstallAppProblem.MESSAGE);
		json.extractingPath("$.installUrl").isEqualTo("https://github.com/apps/vaga-viva/installations/new");
		verify(provisioner, never()).provision(any(), any(), any());
		assertThat(enrollments.countByClassroom(classrooms.byJoinCode(code).orElseThrow())).isZero();
	}

	@Test
	void unknownJoinCodeIs404() {
		assertThat(join("naoexiste01", "maria", "Maria", true)).hasStatus(HttpStatus.NOT_FOUND);
		verify(provisioner, never()).provision(any(), any(), any());
	}

	@Test
	void joiningRequiresSigningIn() {
		var response = mvc.post()
			.uri("/api/classrooms/" + newClassroom() + "/enrollments")
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"displayName\":\"Ana\",\"consent\":true}")
			.exchange();

		assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	/**
	 * CsrfFilter runs before authorization: an anonymous POST without the token is refused with 403, and only
	 * one that carries it reaches the 401. This is why the invite page asks /api/me first and signs a visitor
	 * in instead of posting the enrollment.
	 */
	@Test
	void anAnonymousJoinIsRefusedForCsrfBeforeItIsUnauthorized() {
		String body = "{\"displayName\":\"Maria\",\"consent\":true}";

		var withoutToken = mvc.post()
			.uri("/api/classrooms/abcdefghij/enrollments")
			.contentType(MediaType.APPLICATION_JSON)
			.content(body);
		var withToken = mvc.post()
			.uri("/api/classrooms/abcdefghij/enrollments")
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body);

		assertThat(withoutToken).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(withToken).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.get().uri("/api/me")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	private String newClassroom() {
		var response = mvc.post()
			.uri("/api/classrooms")
			.with(login("david"))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "text": "%s", "name": "Turma", "teacherEmail": null, "emailConsent": false }""".formatted(POSTING))
			.exchange();
		assertThat(response).hasStatus(HttpStatus.CREATED);
		return (String) assertThat(response).bodyJson().extractingPath("$.joinCode").actual();
	}

	private MvcTestResult join(String code, String login, String name, boolean consent) {
		return mvc.post()
			.uri("/api/classrooms/" + code + "/enrollments")
			.with(login(login))
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{ "displayName": "%s", "consent": %s }""".formatted(name, consent))
			.exchange();
	}

	private static ProvisionedRepository repository(String login) {
		return new ProvisionedRepository(login + "/vaga-viva-clinic", "https://github.com/" + login + "/vaga-viva-clinic",
				List.of());
	}

	private OAuth2LoginRequestPostProcessor login(String login) {
		return oauth2Login().clientRegistration(registrations.findByRegistrationId("github"))
			.attributes((attributes) -> {
				attributes.put("id", 42);
				attributes.put("login", login);
			});
	}

}
