package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2LoginRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

/**
 * The teacher's dashboard and the student's card, seeded straight into the database with a fixed clock.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "vagaviva.classroom.teachers=david")
class DashboardApiTests {

	private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

	private static final String POSTING = "Desenvolvedora Java Júnior com Spring Boot, PostgreSQL e Docker.";

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClock {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}

	}

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

	@Test
	void groupsStudentsByMissionWithCounts() {
		Classroom classroom = newClassroom("David");
		List<String> ids = StoredPlan.fromJson(classroom.getPlan()).missionIds();
		enroll(classroom, "ana", "Ana Lima", ids.get(1), NOW.minus(Duration.ofDays(4)).minus(Duration.ofHours(2)),
				NOW.minus(Duration.ofDays(1)), "[]", "[101, 102, 103]", false);
		enroll(classroom, "bia", "Bia Costa", ids.get(0), NOW.minus(Duration.ofDays(6)), NOW.minus(Duration.ofDays(6)),
				"[]", "[]", false);
		enroll(classroom, "caio", "Caio Souza", ids.get(ids.size() - 1), NOW.minus(Duration.ofDays(2)), NOW, "[]", "[]",
				true);

		var response = get("/api/classrooms/" + classroom.getId() + "/dashboard", "david");

		assertThat(response).hasStatusOk();
		var json = assertThat(response).bodyJson();
		json.extractingPath("$.name").isEqualTo("Turma");
		json.extractingPath("$.inviteUrl").isEqualTo("/turma/" + classroom.getJoinCode());
		json.extractingPath("$.counts.stuck").isEqualTo(1);
		json.extractingPath("$.counts.idle").isEqualTo(1);
		json.extractingPath("$.counts.advancing").isEqualTo(0);
		json.extractingPath("$.counts.completed").isEqualTo(1);
		json.extractingPath("$.columns.length()").isEqualTo(ids.size() + 1);
		json.extractingPath("$.columns[0].order").isEqualTo(1);
		json.extractingPath("$.columns[0].missionId").isEqualTo(ids.get(0));
		json.extractingPath("$.columns[0].title").isEqualTo(titleOf(classroom, ids.get(0)));
		json.extractingPath("$.columns[0].students.length()").isEqualTo(1);
		json.extractingPath("$.columns[0].students[0].login").isEqualTo("bia");
		json.extractingPath("$.columns[0].students[0].status").isEqualTo("IDLE");
		json.extractingPath("$.columns[0].students[0].reason").isEqualTo("sem atividade há 6 dias");
		json.extractingPath("$.columns[1].students[0].login").isEqualTo("ana");
		json.extractingPath("$.columns[1].students[0].displayName").isEqualTo("Ana Lima");
		json.extractingPath("$.columns[1].students[0].status").isEqualTo("STUCK");
		json.extractingPath("$.columns[1].students[0].daysOnMission").isEqualTo(4);
		json.extractingPath("$.columns[1].students[0].missionOrder").isEqualTo(2);
		int last = ids.size();
		json.extractingPath("$.columns[" + last + "].order").isNull();
		json.extractingPath("$.columns[" + last + "].missionId").isNull();
		json.extractingPath("$.columns[" + last + "].title").isEqualTo("Concluído");
		json.extractingPath("$.columns[" + last + "].students.length()").isEqualTo(1);
		json.extractingPath("$.columns[" + last + "].students[0].login").isEqualTo("caio");
		json.extractingPath("$.columns[" + last + "].students[0].reason").isEqualTo("todas as missões passando");
	}

	@Test
	void stuckReasonSaysDaysAndPullRequests() {
		Classroom classroom = newClassroom("david");
		List<String> ids = StoredPlan.fromJson(classroom.getPlan()).missionIds();
		enroll(classroom, "ana", "Ana", ids.get(1), NOW.minus(Duration.ofDays(4)).minus(Duration.ofHours(2)),
				NOW.minus(Duration.ofDays(1)), "[]", "[11, 12, 13]", false);
		enroll(classroom, "leo", "Leo", ids.get(0), NOW.minus(Duration.ofDays(3)), NOW.minus(Duration.ofHours(5)),
				"[]", "[7]", false);

		var json = assertThat(get("/api/classrooms/" + classroom.getId() + "/dashboard", "david")).bodyJson();

		json.extractingPath("$.columns[1].students[0].reason").isEqualTo("4 dias na missão 2, 3 PRs nesse período");
		json.extractingPath("$.columns[0].students[0].reason").isEqualTo("3 dias na missão 1, 1 PR nesse período");
	}

	@Test
	void advancingReasonSaysDaysOrToday() {
		Classroom classroom = newClassroom("david");
		List<String> ids = StoredPlan.fromJson(classroom.getPlan()).missionIds();
		enroll(classroom, "novo", "Novo", ids.get(0), NOW.minus(Duration.ofHours(3)), NOW.minus(Duration.ofHours(1)),
				"[]", "[]", false);
		enroll(classroom, "um", "Um Dia", ids.get(0), NOW.minus(Duration.ofHours(30)), NOW.minus(Duration.ofHours(1)),
				"[]", "[]", false);

		var json = assertThat(get("/api/classrooms/" + classroom.getId() + "/dashboard", "david")).bodyJson();

		json.extractingPath("$.counts.advancing").isEqualTo(2);
		// Students keep the order in which they joined: "um" arrived first.
		json.extractingPath("$.columns[0].students[0].reason").isEqualTo("há 1 dia na missão");
		json.extractingPath("$.columns[0].students[1].reason").isEqualTo("hoje");
	}

	@Test
	void studentCardShowsWhatHoldsThemBack() {
		Classroom classroom = newClassroom("david");
		List<String> ids = StoredPlan.fromJson(classroom.getPlan()).missionIds();
		Enrollment enrollment = enroll(classroom, "ana", "Ana Lima", ids.get(1),
				NOW.minus(Duration.ofDays(4)).minus(Duration.ofHours(2)), NOW.minus(Duration.ofDays(1)),
				"[{\"name\":\"createsAppointment\",\"message\":\"expected 201 but was 500\"},{\"name\":\"rejectsPastDate\",\"message\":null}]",
				"[11, 12, 13]", false);
		EnrollmentProgress progress = progresses.findById(enrollment.getId()).orElseThrow();
		progress.setLastReviewerQuestion("O que acontece se a data já passou?");
		progress.setLastPullRequestUrl("https://github.com/ana/vaga-viva-clinic/pull/13");
		progresses.save(progress);

		var response = get("/api/classrooms/" + classroom.getId() + "/students/ana", "david");

		assertThat(response).hasStatusOk();
		var json = assertThat(response).bodyJson();
		json.extractingPath("$.displayName").isEqualTo("Ana Lima");
		json.extractingPath("$.login").isEqualTo("ana");
		json.extractingPath("$.repositoryUrl").isEqualTo("https://github.com/" + enrollment.getRepositoryFullName());
		json.extractingPath("$.missionOrder").isEqualTo(2);
		json.extractingPath("$.missionTitle").isEqualTo(titleOf(classroom, ids.get(1)));
		json.extractingPath("$.status").isEqualTo("STUCK");
		json.extractingPath("$.reason").isEqualTo("4 dias na missão 2, 3 PRs nesse período");
		json.extractingPath("$.failingTests.length()").isEqualTo(2);
		json.extractingPath("$.failingTests[0].name").isEqualTo("createsAppointment");
		json.extractingPath("$.failingTests[0].message").isEqualTo("expected 201 but was 500");
		json.extractingPath("$.failingTests[1].message").isNull();
		json.extractingPath("$.lastReviewerQuestion").isEqualTo("O que acontece se a data já passou?");
		json.extractingPath("$.hintsOnMission").isNull();
		json.extractingPath("$.lastPullRequestUrl").isEqualTo("https://github.com/ana/vaga-viva-clinic/pull/13");
	}

	@Test
	void anotherTeacherGets404() {
		Classroom classroom = newClassroom("david");
		enroll(classroom, "ana", "Ana", StoredPlan.fromJson(classroom.getPlan()).missionIds().get(0), NOW, NOW, "[]",
				"[]", false);

		assertThat(get("/api/classrooms/" + classroom.getId() + "/dashboard", "outra")).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get("/api/classrooms/" + classroom.getId() + "/students/ana", "outra"))
			.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void theOwnerCheckIgnoresCase() {
		Classroom classroom = newClassroom("David");
		enroll(classroom, "ana", "Ana", StoredPlan.fromJson(classroom.getPlan()).missionIds().get(0), NOW, NOW, "[]",
				"[]", false);

		assertThat(get("/api/classrooms/" + classroom.getId() + "/dashboard", "david")).hasStatusOk();
		assertThat(get("/api/classrooms/" + classroom.getId() + "/students/ana", "DAVID")).hasStatusOk();
	}

	@Test
	void unknownClassroomsAndStudentsGet404() {
		Classroom classroom = newClassroom("david");

		assertThat(get("/api/classrooms/" + UUID.randomUUID() + "/dashboard", "david")).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get("/api/classrooms/not-a-uuid/dashboard", "david")).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get("/api/classrooms/" + UUID.randomUUID() + "/students/ana", "david"))
			.hasStatus(HttpStatus.NOT_FOUND);
		assertThat(get("/api/classrooms/" + classroom.getId() + "/students/ninguem", "david"))
			.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void requiresSigningIn() {
		Classroom classroom = newClassroom("david");

		assertThat(mvc.get().uri("/api/classrooms/" + classroom.getId() + "/dashboard")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.get().uri("/api/classrooms/" + classroom.getId() + "/students/ana"))
			.hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void aClassroomWithoutStudentsHasEmptyColumnsAndZeroCounts() {
		Classroom classroom = newClassroom("david");
		int missions = StoredPlan.fromJson(classroom.getPlan()).missionIds().size();

		var json = assertThat(get("/api/classrooms/" + classroom.getId() + "/dashboard", "david")).bodyJson();

		json.extractingPath("$.counts.stuck").isEqualTo(0);
		json.extractingPath("$.counts.idle").isEqualTo(0);
		json.extractingPath("$.counts.advancing").isEqualTo(0);
		json.extractingPath("$.counts.completed").isEqualTo(0);
		json.extractingPath("$.columns.length()").isEqualTo(missions + 1);
		for (int i = 0; i <= missions; i++) {
			json.extractingPath("$.columns[" + i + "].students.length()").isEqualTo(0);
		}
	}

	@Test
	void completedStudentsAppearOnlyInTheLastColumn() {
		Classroom classroom = newClassroom("david");
		List<String> ids = StoredPlan.fromJson(classroom.getPlan()).missionIds();
		// The stored current mission of a finished student is still the last one: it must not show up there.
		enroll(classroom, "caio", "Caio", ids.get(ids.size() - 1), NOW.minus(Duration.ofDays(9)), NOW, "[]", "[]",
				true);

		var json = assertThat(get("/api/classrooms/" + classroom.getId() + "/dashboard", "david")).bodyJson();

		for (int i = 0; i < ids.size(); i++) {
			json.extractingPath("$.columns[" + i + "].students.length()").isEqualTo(0);
		}
		json.extractingPath("$.columns[" + ids.size() + "].students.length()").isEqualTo(1);
		json.extractingPath("$.counts.completed").isEqualTo(1);
	}

	@Test
	void malformedStoredJsonDoesNotBreakTheDashboardOrTheCard() {
		Classroom classroom = newClassroom("david");
		List<String> ids = StoredPlan.fromJson(classroom.getPlan()).missionIds();
		enroll(classroom, "ana", "Ana", ids.get(0), NOW.minus(Duration.ofDays(4)), NOW.minus(Duration.ofDays(1)),
				"{\"name\":\"not a list\"}", "\"oops\"", false);
		enroll(classroom, "bia", "Bia", ids.get(0), NOW.minus(Duration.ofDays(1)), NOW, "[\"a\", 3]", "[\"x\"]", false);

		var dashboard = get("/api/classrooms/" + classroom.getId() + "/dashboard", "david");
		var ana = get("/api/classrooms/" + classroom.getId() + "/students/ana", "david");
		var bia = get("/api/classrooms/" + classroom.getId() + "/students/bia", "david");

		assertThat(dashboard).hasStatusOk();
		assertThat(dashboard).bodyJson()
			.extractingPath("$.columns[0].students[0].reason")
			.isEqualTo("4 dias na missão 1, 0 PRs nesse período");
		assertThat(ana).hasStatusOk();
		assertThat(ana).bodyJson().extractingPath("$.failingTests.length()").isEqualTo(0);
		assertThat(bia).hasStatusOk();
		assertThat(bia).bodyJson().extractingPath("$.failingTests.length()").isEqualTo(0);
	}

	private Classroom newClassroom(String teacherLogin) {
		return classrooms.create(teacherLogin, POSTING, "Turma", null, false);
	}

	private String titleOf(Classroom classroom, String missionId) {
		StoredPlan stored = StoredPlan.fromJson(classroom.getPlan());
		return classrooms.catalogOf(stored)
			.missions()
			.stream()
			.filter((m) -> m.id().equals(missionId))
			.findFirst()
			.orElseThrow()
			.title();
	}

	private Enrollment enroll(Classroom classroom, String login, String name, String missionId, Instant since,
			Instant lastActivity, String failingTests, String pullRequests, boolean completed) {
		Enrollment enrollment = enrollments.saveAndFlush(new Enrollment(UUID.randomUUID(), classroom, login, name,
				login + "/" + classroom.getJoinCode(), since, since));
		EnrollmentProgress progress = new EnrollmentProgress(enrollment);
		progress.setCurrentMissionId(missionId);
		progress.setCurrentMissionSince(since);
		progress.setLastActivityAt(lastActivity);
		progress.setCompleted(completed);
		progress.setFailingTests(failingTests);
		progress.setPullRequestsOnMission(pullRequests);
		progress.setHintsByMission("{}");
		progresses.saveAndFlush(progress);
		return enrollment;
	}

	private MvcTestResult get(String uri, String login) {
		return mvc.get().uri(uri).with(login(login)).exchange();
	}

	private OAuth2LoginRequestPostProcessor login(String login) {
		return oauth2Login().clientRegistration(registrations.findByRegistrationId("github"))
			.attributes((attributes) -> {
				attributes.put("id", 42);
				attributes.put("login", login);
			});
	}

}
