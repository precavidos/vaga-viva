package io.github.precavidos.vagaviva.classroom;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface EnrollmentProgressRepository extends JpaRepository<EnrollmentProgress, UUID> {

}
