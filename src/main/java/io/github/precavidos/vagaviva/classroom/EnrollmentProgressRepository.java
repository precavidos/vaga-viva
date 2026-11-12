package io.github.precavidos.vagaviva.classroom;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface EnrollmentProgressRepository extends JpaRepository<EnrollmentProgress, UUID> {

	/** Locks the row until the transaction ends, so concurrent events update one student one at a time. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from EnrollmentProgress p where p.id = :id")
	Optional<EnrollmentProgress> findForUpdate(@Param("id") UUID id);

}
