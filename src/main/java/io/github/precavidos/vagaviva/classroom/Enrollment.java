package io.github.precavidos.vagaviva.classroom;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A student who joined a classroom and consented to the teacher seeing their progress (ADR 0007). */
@Entity
@Table(name = "enrollment")
class Enrollment {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "classroom_id", nullable = false)
	private Classroom classroom;

	@Column(name = "student_login", nullable = false)
	private String studentLogin;

	@Column(name = "display_name", nullable = false, length = 40)
	private String displayName;

	@Column(name = "repository_full_name", nullable = false, unique = true)
	private String repositoryFullName;

	@Column(name = "consented_at", nullable = false)
	private Instant consentedAt;

	@Column(name = "enrolled_at", nullable = false)
	private Instant enrolledAt;

	protected Enrollment() {
	}

	Enrollment(UUID id, Classroom classroom, String studentLogin, String displayName, String repositoryFullName,
			Instant consentedAt, Instant enrolledAt) {
		this.id = id;
		this.classroom = classroom;
		this.studentLogin = studentLogin;
		this.displayName = displayName;
		this.repositoryFullName = repositoryFullName;
		this.consentedAt = consentedAt;
		this.enrolledAt = enrolledAt;
	}

	UUID getId() {
		return id;
	}

	Classroom getClassroom() {
		return classroom;
	}

	String getStudentLogin() {
		return studentLogin;
	}

	String getDisplayName() {
		return displayName;
	}

	String getRepositoryFullName() {
		return repositoryFullName;
	}

	Instant getConsentedAt() {
		return consentedAt;
	}

	Instant getEnrolledAt() {
		return enrolledAt;
	}

}
