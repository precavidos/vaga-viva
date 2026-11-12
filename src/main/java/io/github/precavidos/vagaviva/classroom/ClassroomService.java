package io.github.precavidos.vagaviva.classroom;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import io.github.precavidos.vagaviva.jobposting.JobPostingExtractor;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.plan.ProjectPlanners;

/**
 * Creates classrooms from a base posting and finds them. The posting is planned
 * once, here; only the plan's ids are stored (ADR 0003).
 */
@Service
class ClassroomService {

	static final int MAX_NAME_LENGTH = 80;

	private final ClassroomRepository classrooms;

	private final EnrollmentRepository enrollments;

	private final JobPostingExtractor extractor;

	private final ProjectPlanners planners;

	private final ClassroomProperties properties;

	private final Clock clock;

	private final Map<String, MissionCatalog> catalogs;

	ClassroomService(ClassroomRepository classrooms, EnrollmentRepository enrollments, JobPostingExtractor extractor,
			ProjectPlanners planners, ClassroomProperties properties, Clock clock, List<MissionCatalog> catalogs) {
		this.classrooms = classrooms;
		this.enrollments = enrollments;
		this.extractor = extractor;
		this.planners = planners;
		this.properties = properties;
		this.clock = clock;
		this.catalogs = catalogs.stream().collect(Collectors.toMap(MissionCatalog::archetype, Function.identity()));
	}

	/**
	 * Creates the classroom of an allowed teacher.
	 * @throws NotATeacherException when the login is not an allowed teacher
	 * @throws InvalidClassroomException when the name, the e-mail consent or the plan is not acceptable
	 */
	Classroom create(String teacherLogin, String postingText, String name, String teacherEmail,
			boolean emailConsent) {
		if (!properties.teachers().contains(teacherLogin)) {
			throw new NotATeacherException();
		}
		String cleanName = name == null ? "" : name.strip();
		if (cleanName.isEmpty() || cleanName.length() > MAX_NAME_LENGTH) {
			throw new InvalidClassroomException("O nome da turma deve ter de 1 a " + MAX_NAME_LENGTH + " caracteres.");
		}
		String email = teacherEmail == null || teacherEmail.isBlank() ? null : teacherEmail.strip();
		if (email != null && !emailConsent) {
			throw new InvalidClassroomException("Para receber o resumo por e-mail, aceite o consentimento.");
		}
		StoredPlan plan = StoredPlan.of(planners.plan(extractor.extract(postingText)));
		if (plan.missions().isEmpty()) {
			// An empty plan would count as completed by the mission-progress rules.
			throw new InvalidClassroomException("Não foi possível montar missões a partir dessa vaga.");
		}
		var now = clock.instant();
		var classroom = new Classroom(UUID.randomUUID(), newJoinCode(), cleanName, teacherLogin, email,
				email == null ? null : now, plan.toJson(), now);
		return classrooms.save(classroom);
	}

	Optional<Classroom> byJoinCode(String joinCode) {
		return classrooms.findByJoinCode(joinCode);
	}

	List<Classroom> ofTeacher(String teacherLogin) {
		return classrooms.findByTeacherLoginOrderByCreatedAtDesc(teacherLogin);
	}

	long studentCount(Classroom classroom) {
		return enrollments.countByClassroom(classroom);
	}

	/** The catalog of the stored plan's archetype, to turn it back into missions. */
	MissionCatalog catalogOf(StoredPlan plan) {
		MissionCatalog catalog = catalogs.get(plan.archetype());
		if (catalog == null) {
			throw new IllegalStateException("No mission catalog for archetype " + plan.archetype());
		}
		return catalog;
	}

	private String newJoinCode() {
		String code;
		do {
			code = JoinCodes.next();
		}
		while (classrooms.findByJoinCode(code).isPresent());
		return code;
	}

	static class NotATeacherException extends RuntimeException {

	}

	static class InvalidClassroomException extends RuntimeException {

		InvalidClassroomException(String message) {
			super(message);
		}

	}

}
