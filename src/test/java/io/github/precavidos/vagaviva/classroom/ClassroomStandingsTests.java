package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;
import io.github.precavidos.vagaviva.classroom.ClassroomStandings.Standing;
import jakarta.persistence.EntityManager;

@DataJpaTest
@Import({ TestcontainersConfiguration.class, ClassroomStandings.class })
@AutoConfigureTestDatabase(replace = NONE)
class ClassroomStandingsTests {

	private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

	private static final String REPO = "maria/vaga-viva-clinic-scheduling";

	@Autowired
	ClassroomStandings standings;

	@Autowired
	ClassroomRepository classrooms;

	@Autowired
	EnrollmentRepository enrollments;

	@Autowired
	EntityManager entityManager;

	@Test
	void tellsTheMissionOfAnEnrolledStudent() {
		Classroom classroom = classrooms
			.saveAndFlush(new Classroom(UUID.randomUUID(), "ABC123", "Turma 1", "profa", null, null, "{}", NOW));
		Enrollment enrollment = enrollments
			.saveAndFlush(new Enrollment(UUID.randomUUID(), classroom, "maria", "Maria", REPO, NOW, NOW));
		EnrollmentProgress progress = new EnrollmentProgress(enrollment);
		progress.setCurrentMissionId("patients-api");
		progress.setFailingTests("[]");
		progress.setPullRequestsOnMission("[]");
		progress.setHintsByMission("{}");
		entityManager.persist(progress);
		entityManager.flush();
		entityManager.clear();

		assertThat(standings.forRepository(REPO)).contains(new Standing("patients-api", false));
	}

	@Test
	void unknownRepositoryIsEmpty() {
		assertThat(standings.forRepository("someone/else")).isEmpty();
	}

}
