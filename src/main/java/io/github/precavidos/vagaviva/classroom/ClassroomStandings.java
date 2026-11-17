package io.github.precavidos.vagaviva.classroom;

import java.util.Optional;

import org.springframework.stereotype.Component;

/** Where a classroom student is in the plan, read from the progress the webhook events keep. */
@Component
public class ClassroomStandings {

	private final EnrollmentRepository enrollments;

	private final EnrollmentProgressRepository progress;

	ClassroomStandings(EnrollmentRepository enrollments, EnrollmentProgressRepository progress) {
		this.enrollments = enrollments;
		this.progress = progress;
	}

	/** Empty when the repository is not a classroom student's or has no progress yet. */
	public Optional<Standing> forRepository(String repositoryFullName) {
		return enrollments.findByRepositoryFullName(repositoryFullName)
			.flatMap(enrollment -> progress.findById(enrollment.getId()))
			.map(p -> new Standing(p.getCurrentMissionId(), p.isCompleted()));
	}

	/** The mission the student is on, or whether the whole plan is done. */
	public record Standing(String currentMissionId, boolean completed) {
	}

}
