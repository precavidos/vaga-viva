package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;
import io.github.precavidos.vagaviva.classroom.StoredPlan.StoredMission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.TestReport;
import jakarta.persistence.EntityManager;

/** How the webhook events move a classroom student along the stored plan. */
@DataJpaTest
@Import({ TestcontainersConfiguration.class, ProgressRecorder.class, ProgressRecorderTests.FixedClock.class })
@AutoConfigureTestDatabase(replace = NONE)
class ProgressRecorderTests {

	private static final Instant START = Instant.parse("2026-11-12T10:00:00Z");

	private static final Instant LATER = Instant.parse("2026-11-12T12:00:00Z");

	private static final String REPO = "maria/vaga-viva-clinic-scheduling";

	private static final Set<String> BOTH = Set.of("patients-api", "persistence");

	@TestConfiguration
	static class FixedClock {

		@Bean
		Clock clock() {
			return Clock.fixed(LATER, ZoneOffset.UTC);
		}

	}

	@Autowired
	ProgressRecorder recorder;

	@Autowired
	ClassroomRepository classrooms;

	@Autowired
	EnrollmentRepository enrollments;

	@Autowired
	EntityManager entityManager;

	private UUID enrollmentId;

	@BeforeEach
	void setUp() {
		String plan = new StoredPlan("java-spring", null, null, 0,
				List.of(new StoredMission("patients-api", null, List.of()),
						new StoredMission("persistence", null, List.of())),
				List.of(), List.of())
			.toJson();
		Classroom classroom = classrooms
			.saveAndFlush(new Classroom(UUID.randomUUID(), "ABC123", "Turma 1", "profa", null, null, plan, START));
		Enrollment enrollment = enrollments
			.saveAndFlush(new Enrollment(UUID.randomUUID(), classroom, "maria", "Maria", REPO, START, START));
		EnrollmentProgress progress = new EnrollmentProgress(enrollment);
		progress.setCurrentMissionId("patients-api");
		progress.setCurrentMissionSince(START);
		progress.setLastActivityAt(START);
		progress.setFailingTests("[]");
		progress.setPullRequestsOnMission("[]");
		progress.setHintsByMission("{}");
		entityManager.persist(progress);
		entityManager.flush();
		entityManager.clear();
		enrollmentId = enrollment.getId();
	}

	@Test
	void tracksOnlyRepositoriesOfAClassroom() {
		assertThat(recorder.tracks(REPO)).isTrue();
		assertThat(recorder.tracks("other/repository")).isFalse();
	}

	@Test
	void movesToTheNextMissionAndResetsTheTimer() {
		recorder.testsRan(REPO, 3, report(BOTH, failed("persistence", "savesIt", "expected 1")));

		EnrollmentProgress progress = reload();
		assertThat(progress.getCurrentMissionId()).isEqualTo("persistence");
		assertThat(progress.getCurrentMissionSince()).isEqualTo(LATER);
		assertThat(progress.getPullRequestsOnMission()).isEqualTo("[3]");
		assertThat(progress.getLastPullRequestUrl()).isEqualTo("https://github.com/" + REPO + "/pull/3");
		assertThat(progress.getLastActivityAt()).isEqualTo(LATER);
		assertThat(progress.isCompleted()).isFalse();
		assertThat(progress.getFailingTests()).contains("savesIt").contains("expected 1");
	}

	@Test
	void staysOnTheMissionAndCountsPullRequests() {
		var report = report(BOTH, failed("patients-api", "createsIt", "expected 201"));

		recorder.testsRan(REPO, 3, report);
		recorder.testsRan(REPO, 3, report);
		recorder.testsRan(REPO, 4, report);
		recorder.testsRan(REPO, null, report);

		EnrollmentProgress progress = reload();
		assertThat(progress.getCurrentMissionId()).isEqualTo("patients-api");
		assertThat(progress.getCurrentMissionSince()).isEqualTo(START);
		assertThat(progress.getPullRequestsOnMission()).isEqualTo("[3, 4]");
		assertThat(progress.getLastPullRequestUrl()).endsWith("/pull/4");
	}

	@Test
	void partialReportOnlyRecordsActivity() {
		recorder.testsRan(REPO, 3, report(Set.of("persistence"), failed("persistence", "savesIt", "expected 1")));

		EnrollmentProgress progress = reload();
		assertThat(progress.getCurrentMissionId()).isEqualTo("patients-api");
		assertThat(progress.getCurrentMissionSince()).isEqualTo(START);
		assertThat(progress.isCompleted()).isFalse();
		assertThat(progress.getFailingTests()).isEqualTo("[]");
		assertThat(progress.getPullRequestsOnMission()).isEqualTo("[]");
		assertThat(progress.getLastActivityAt()).isEqualTo(LATER);
	}

	@Test
	void completedWhenEveryMissionPasses() {
		recorder.testsRan(REPO, 3, report(BOTH));

		EnrollmentProgress progress = reload();
		assertThat(progress.isCompleted()).isTrue();
		assertThat(progress.getFailingTests()).isEqualTo("[]");
		assertThat(progress.getLastActivityAt()).isEqualTo(LATER);
	}

	@Test
	void messagesThatLookLikeCodeAreDropped() {
		recorder.testsRan(REPO, 3, report(BOTH, failed("patients-api", "createsIt", "expected: return x;")));

		assertThat(reload().getFailingTests()).contains("createsIt").contains("\"message\": null");
	}

	@Test
	void longMessagesAreCutAt200() {
		recorder.testsRan(REPO, 3, report(BOTH, failed("patients-api", "createsIt", "a".repeat(500))));

		assertThat(reload().getFailingTests()).contains("a".repeat(200)).doesNotContain("a".repeat(201));
	}

	@Test
	void keepsTheReviewerQuestionOnlyWhenItIsSafe() {
		recorder.reviewerAsked(REPO, 3, "O que o teste espera receber?");
		recorder.reviewerAsked(REPO, 3, "Tente:\n```java\nreturn x;\n```");

		assertThat(reload().getLastReviewerQuestion()).isEqualTo("O que o teste espera receber?");
	}

	@Test
	void ignoresRepositoriesOutsideAClassroom() {
		recorder.testsRan("other/repository", 3, report(BOTH));
		recorder.activity("other/repository");
		recorder.reviewerAsked("other/repository", 3, "Uma pergunta?");
		recorder.hintGiven("other/repository", "patients-api");

		EnrollmentProgress progress = reload();
		assertThat(progress.isCompleted()).isFalse();
		assertThat(progress.getLastActivityAt()).isEqualTo(START);
		assertThat(progress.getLastReviewerQuestion()).isNull();
		assertThat(progress.getHintsByMission()).isEqualTo("{}");
	}

	@Test
	void countsHintsPerMission() {
		recorder.hintGiven(REPO, "patients-api");
		recorder.hintGiven(REPO, "patients-api");
		recorder.hintGiven(REPO, "persistence");

		assertThat(reload().getHintsByMission()).contains("\"patients-api\": 2").contains("\"persistence\": 1");
	}

	@Test
	void activityMovesOnlyTheLastActivity() {
		recorder.activity(REPO);

		EnrollmentProgress progress = reload();
		assertThat(progress.getLastActivityAt()).isEqualTo(LATER);
		assertThat(progress.getCurrentMissionId()).isEqualTo("patients-api");
	}

	@Test
	void storedJsonOfTheWrongShapeDoesNotBlockRecordingForever() {
		EnrollmentProgress stored = entityManager.find(EnrollmentProgress.class, enrollmentId);
		stored.setHintsByMission("[1]");
		stored.setPullRequestsOnMission("{}");
		entityManager.flush();
		entityManager.clear();

		recorder.hintGiven(REPO, "patients-api");
		recorder.testsRan(REPO, 5, report(BOTH, failed("patients-api", "createsIt", "expected 201")));

		EnrollmentProgress progress = reload();
		assertThat(progress.getHintsByMission()).isEqualTo("{\"patients-api\": 1}");
		assertThat(progress.getPullRequestsOnMission()).isEqualTo("[5]");
	}

	@Test
	void aFailureInsideAnEventIsSwallowed() {
		String broken = "{\"archetype\":\"java-spring\",\"missions\":[\"not-an-object\"]}";
		Classroom classroom = classrooms
			.saveAndFlush(new Classroom(UUID.randomUUID(), "BROKEN", "Turma 2", "profa", null, null, broken, START));
		Enrollment enrollment = enrollments.saveAndFlush(
				new Enrollment(UUID.randomUUID(), classroom, "joao", "Joao", "joao/broken-repository", START, START));
		EnrollmentProgress progress = new EnrollmentProgress(enrollment);
		progress.setLastActivityAt(START);
		entityManager.persist(progress);
		entityManager.flush();

		recorder.testsRan("joao/broken-repository", 1, report(BOTH));

		assertThat(recorder.tracks("joao/broken-repository")).isTrue();
	}

	private EnrollmentProgress reload() {
		entityManager.flush();
		entityManager.clear();
		return entityManager.find(EnrollmentProgress.class, enrollmentId);
	}

	private static TestReport report(Set<String> run, FailedTest... failed) {
		return new TestReport(run, List.of(failed));
	}

	private static FailedTest failed(String mission, String test, String message) {
		return new FailedTest(mission, test, message);
	}

}
