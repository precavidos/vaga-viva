package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;
import io.github.precavidos.vagaviva.classroom.StoredPlan.StoredMission;

/** Events arrive on a thread pool: concurrent updates of one student must not overwrite each other. */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({ TestcontainersConfiguration.class, ProgressRecorder.class, ProgressRecorderConcurrencyTests.FixedClock.class })
@AutoConfigureTestDatabase(replace = NONE)
class ProgressRecorderConcurrencyTests {

	private static final Instant NOW = Instant.parse("2026-11-12T12:00:00Z");

	private static final String REPO = "maria/vaga-viva-concurrency";

	@TestConfiguration
	static class FixedClock {

		@Bean
		Clock clock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}

	}

	@Autowired
	ProgressRecorder recorder;

	@Autowired
	ClassroomRepository classrooms;

	@Autowired
	EnrollmentRepository enrollments;

	@Autowired
	EnrollmentProgressRepository progresses;

	private Classroom classroom;

	private Enrollment enrollment;

	@AfterEach
	void cleanUp() {
		if (enrollment != null) {
			progresses.deleteById(enrollment.getId());
			enrollments.deleteById(enrollment.getId());
		}
		if (classroom != null) {
			classrooms.deleteById(classroom.getId());
		}
	}

	@Test
	void concurrentHintsAreAllCounted() throws Exception {
		enroll();

		runConcurrently(20, () -> recorder.hintGiven(REPO, "patients-api"));

		assertThat(progresses.findById(enrollment.getId()).orElseThrow().getHintsByMission())
			.isEqualTo("{\"patients-api\": 20}");
	}

	private void enroll() {
		String plan = new StoredPlan("java-spring", null, null, 0,
				List.of(new StoredMission("patients-api", null, List.of())), List.of(), List.of())
			.toJson();
		classroom = classrooms
			.saveAndFlush(new Classroom(UUID.randomUUID(), "CONC01", "Turma", "profa", null, null, plan, NOW));
		enrollment = enrollments
			.saveAndFlush(new Enrollment(UUID.randomUUID(), classroom, "maria", "Maria", REPO, NOW, NOW));
		EnrollmentProgress progress = new EnrollmentProgress(enrollment);
		progress.setFailingTests("[]");
		progress.setPullRequestsOnMission("[]");
		progress.setHintsByMission("{}");
		progresses.saveAndFlush(progress);
	}

	private static void runConcurrently(int threads, Runnable action) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			CountDownLatch start = new CountDownLatch(1);
			List<Future<?>> done = new ArrayList<>();
			for (int i = 0; i < threads; i++) {
				done.add(pool.submit(() -> {
					start.await();
					action.run();
					return null;
				}));
			}
			start.countDown();
			for (Future<?> future : done) {
				future.get(30, TimeUnit.SECONDS);
			}
		}
		finally {
			pool.shutdownNow();
		}
	}

}
