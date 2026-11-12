package io.github.precavidos.vagaviva.classroom;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

	Optional<Enrollment> findByRepositoryFullName(String repositoryFullName);

	Optional<Enrollment> findByClassroomAndStudentLogin(Classroom classroom, String studentLogin);

	List<Enrollment> findByClassroomOrderByEnrolledAt(Classroom classroom);

}
