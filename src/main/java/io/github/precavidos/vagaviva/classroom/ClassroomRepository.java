package io.github.precavidos.vagaviva.classroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface ClassroomRepository extends JpaRepository<Classroom, UUID> {

	Optional<Classroom> findByJoinCode(String joinCode);

	List<Classroom> findByTeacherLoginOrderByCreatedAtDesc(String teacherLogin);

	/** The classrooms whose teacher gave an e-mail and agreed to the weekly summary. */
	List<Classroom> findByTeacherEmailIsNotNullAndTeacherEmailConsentAtIsNotNullOrderByCreatedAt();

}
