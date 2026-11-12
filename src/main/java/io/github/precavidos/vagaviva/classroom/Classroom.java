package io.github.precavidos.vagaviva.classroom;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A teacher's class: a join code and the plan every student in it follows. */
@Entity
@Table(name = "classroom")
class Classroom {

	@Id
	private UUID id;

	@Column(name = "join_code", nullable = false, unique = true)
	private String joinCode;

	@Column(nullable = false)
	private String name;

	@Column(name = "teacher_login", nullable = false)
	private String teacherLogin;

	@Column(name = "teacher_email")
	private String teacherEmail;

	@Column(name = "teacher_email_consent_at")
	private Instant teacherEmailConsentAt;

	/** The JSON of the stored plan: ids only, never the job posting text. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false, columnDefinition = "jsonb")
	private String plan;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Classroom() {
	}

	Classroom(UUID id, String joinCode, String name, String teacherLogin, String teacherEmail,
			Instant teacherEmailConsentAt, String plan, Instant createdAt) {
		this.id = id;
		this.joinCode = joinCode;
		this.name = name;
		this.teacherLogin = teacherLogin;
		this.teacherEmail = teacherEmail;
		this.teacherEmailConsentAt = teacherEmailConsentAt;
		this.plan = plan;
		this.createdAt = createdAt;
	}

	UUID getId() {
		return id;
	}

	String getJoinCode() {
		return joinCode;
	}

	String getName() {
		return name;
	}

	String getTeacherLogin() {
		return teacherLogin;
	}

	String getTeacherEmail() {
		return teacherEmail;
	}

	Instant getTeacherEmailConsentAt() {
		return teacherEmailConsentAt;
	}

	String getPlan() {
		return plan;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

}
