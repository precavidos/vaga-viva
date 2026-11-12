package io.github.precavidos.vagaviva.classroom;

import java.time.Instant;
import java.util.UUID;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = NONE)
@TestPropertySource(properties = "logging.level.org.hibernate.orm.jdbc.error=OFF")
class ClassroomRepositoryTests {

	private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

	@Autowired
	ClassroomRepository classrooms;

	@Autowired
	EnrollmentRepository enrollments;

	@Autowired
	EntityManager entityManager;

	@Test
	void savesAClassroomWithItsEnrollmentAndProgress() {
		Classroom classroom = classrooms.saveAndFlush(classroom("ABC123"));
		Enrollment enrollment = enrollments
			.saveAndFlush(enrollment(classroom, "maria", "maria/vaga-viva-clinic-scheduling"));
		EnrollmentProgress progress = new EnrollmentProgress(enrollment);
		progress.setCurrentMissionId("patients-api");
		progress.setCurrentMissionSince(NOW);
		progress.setLastActivityAt(NOW);
		progress.setFailingTests("[{\"name\":\"createsPatient\",\"message\":\"expected 201\"}]");
		progress.setPullRequestsOnMission("[7]");
		progress.setLastReviewerQuestion("Why this status code?");
		progress.setLastPullRequestUrl("https://github.com/maria/vaga-viva-clinic-scheduling/pull/7");
		progress.setHintsByMission("{\"patients-api\":2}");
		entityManager.persist(progress);
		entityManager.flush();
		entityManager.clear();

		assertThat(classrooms.findByJoinCode("ABC123")).get().extracting(Classroom::getName).isEqualTo("Turma 1");
		assertThat(classrooms.findByTeacherLoginOrderByCreatedAtDesc("profa")).hasSize(1);
		Enrollment found = enrollments.findByRepositoryFullName("maria/vaga-viva-clinic-scheduling").orElseThrow();
		assertThat(found.getId()).isEqualTo(enrollment.getId());
		assertThat(enrollments.findByClassroomAndStudentLogin(classroom, "maria")).isPresent();
		assertThat(enrollments.findByClassroomOrderByEnrolledAt(classroom)).hasSize(1);
		EnrollmentProgress reloaded = entityManager.find(EnrollmentProgress.class, enrollment.getId());
		assertThat(reloaded.getCurrentMissionId()).isEqualTo("patients-api");
		assertThat(reloaded.isCompleted()).isFalse();
		assertThat(reloaded.getPullRequestsOnMission()).isEqualTo("[7]");
		assertThat(reloaded.getHintsByMission()).isEqualTo("{\"patients-api\": 2}");
		assertThat(reloaded.getFailingTests()).isEqualTo("[{\"name\": \"createsPatient\", \"message\": \"expected 201\"}]");
		assertThat(entityManager.find(Classroom.class, classroom.getId()).getPlan())
			.isEqualTo("{\"missions\": [\"patients-api\"], \"archetype\": \"java-spring\"}"); // jsonb orders keys by length

		assertThat(jsonbType("classroom", "plan", "id", classroom.getId())).isEqualTo("object");
		assertThat(jsonbType("enrollment_progress", "failing_tests", "enrollment_id", enrollment.getId()))
			.isEqualTo("array");
		assertThat(jsonbType("enrollment_progress", "pull_requests_on_mission", "enrollment_id", enrollment.getId()))
			.isEqualTo("array");
		assertThat(jsonbType("enrollment_progress", "hints_by_mission", "enrollment_id", enrollment.getId()))
			.isEqualTo("object");
	}

	@Test
	void rejectsTheSameJoinCodeTwice() {
		classrooms.saveAndFlush(classroom("ABC123"));

		assertThatThrownBy(() -> classrooms.saveAndFlush(classroom("ABC123")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsTheSameRepositoryTwice() {
		Classroom classroom = classrooms.saveAndFlush(classroom("ABC123"));
		enrollments.saveAndFlush(enrollment(classroom, "maria", "maria/vaga-viva-clinic-scheduling"));

		assertThatThrownBy(
				() -> enrollments.saveAndFlush(enrollment(classroom, "joao", "maria/vaga-viva-clinic-scheduling")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsTheSameStudentTwiceInAClassroom() {
		Classroom classroom = classrooms.saveAndFlush(classroom("ABC123"));
		enrollments.saveAndFlush(enrollment(classroom, "maria", "maria/vaga-viva-clinic-scheduling"));

		assertThatThrownBy(() -> enrollments.saveAndFlush(enrollment(classroom, "maria", "maria/another-repository")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private String jsonbType(String table, String column, String idColumn, UUID id) {
		return (String) entityManager
			.createNativeQuery("select jsonb_typeof(" + column + ") from " + table + " where " + idColumn + " = ?1")
			.setParameter(1, id)
			.getSingleResult();
	}

	private static Classroom classroom(String joinCode) {
		return new Classroom(UUID.randomUUID(), joinCode, "Turma 1", "profa", null, null,
				"{\"archetype\":\"java-spring\",\"missions\":[\"patients-api\"]}", NOW);
	}

	private static Enrollment enrollment(Classroom classroom, String login, String repository) {
		return new Enrollment(UUID.randomUUID(), classroom, login, "Maria", repository, NOW, NOW);
	}

}
