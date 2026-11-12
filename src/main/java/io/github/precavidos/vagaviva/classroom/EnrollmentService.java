package io.github.precavidos.vagaviva.classroom;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.precavidos.vagaviva.provisioning.StudentRepositoryProvisioner;

/**
 * Enrolls a student in a classroom. The repository is provisioned first, outside
 * any database transaction (the GitHub calls are slow); the enrollment and its
 * first progress row are then saved together in one short transaction.
 */
@Service
class EnrollmentService {

	private static final int LOCKS = 64;

	private final ClassroomService classrooms;

	private final EnrollmentRepository enrollments;

	private final EnrollmentProgressRepository progresses;

	private final StudentRepositoryProvisioner provisioner;

	private final TransactionTemplate transaction;

	private final Clock clock;

	/**
	 * Serializes concurrent joins of the same student, so a double click does not
	 * create two repositories. The database constraints still decide across instances.
	 */
	private final ReentrantLock[] locks = new ReentrantLock[LOCKS];

	EnrollmentService(ClassroomService classrooms, EnrollmentRepository enrollments,
			EnrollmentProgressRepository progresses, StudentRepositoryProvisioner provisioner,
			TransactionTemplate transaction, Clock clock) {
		this.classrooms = classrooms;
		this.enrollments = enrollments;
		this.progresses = progresses;
		this.provisioner = provisioner;
		this.transaction = transaction;
		this.clock = clock;
		for (int i = 0; i < LOCKS; i++) {
			locks[i] = new ReentrantLock();
		}
	}

	Optional<Enrollment> existing(Classroom classroom, String studentLogin) {
		return enrollments.findByClassroomAndStudentLogin(classroom, studentLogin);
	}

	/** The student's enrollment; joining again returns the existing one without provisioning anything. */
	Enrollment enroll(Classroom classroom, String studentLogin, String displayName, String token) {
		return join(classroom, studentLogin, displayName, token).enrollment();
	}

	/** Like {@link #enroll}, also telling whether this call is the one that created the enrollment. */
	Joined join(Classroom classroom, String studentLogin, String displayName, String token) {
		ReentrantLock lock = locks[Math.floorMod((classroom.getId() + "/" + studentLogin).hashCode(), LOCKS)];
		lock.lock();
		try {
			Optional<Enrollment> already = existing(classroom, studentLogin);
			if (already.isPresent()) {
				return new Joined(already.get(), false);
			}
			StoredPlan stored = StoredPlan.fromJson(classroom.getPlan());
			var plan = stored.toProjectPlan(classrooms.catalogOf(stored));
			var repository = provisioner.provision(token, studentLogin, plan);
			try {
				Enrollment saved = transaction.execute((status) -> save(classroom, studentLogin, displayName,
						repository.fullName(), stored.missionIds().get(0)));
				return new Joined(saved, true);
			}
			catch (DataIntegrityViolationException e) {
				// Another instance enrolled the same student first: theirs wins.
				return new Joined(existing(classroom, studentLogin).orElseThrow(() -> e), false);
			}
		}
		finally {
			lock.unlock();
		}
	}

	private Enrollment save(Classroom classroom, String studentLogin, String displayName, String repositoryFullName,
			String firstMissionId) {
		Instant now = clock.instant();
		Enrollment enrollment = enrollments.saveAndFlush(new Enrollment(UUID.randomUUID(), classroom, studentLogin,
				displayName, repositoryFullName, now, now));
		EnrollmentProgress progress = new EnrollmentProgress(enrollment);
		progress.setCurrentMissionId(firstMissionId);
		progress.setCurrentMissionSince(now);
		progress.setLastActivityAt(now);
		progress.setFailingTests("[]");
		progress.setPullRequestsOnMission("[]");
		progress.setHintsByMission("{}");
		progresses.saveAndFlush(progress);
		return enrollment;
	}

	record Joined(Enrollment enrollment, boolean created) {
	}

}
