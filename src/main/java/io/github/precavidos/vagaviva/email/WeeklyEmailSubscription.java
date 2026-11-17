package io.github.precavidos.vagaviva.email;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A student who consented to the weekly e-mail about their repository (ADR 0003). */
@Entity
@Table(name = "weekly_email_subscription")
class WeeklyEmailSubscription {

	@Id
	@Column(name = "student_login", length = 100)
	private String studentLogin;

	@Column(name = "email", nullable = false, length = 254)
	private String email;

	@Column(name = "repository_full_name", nullable = false, length = 200)
	private String repositoryFullName;

	@Column(name = "consented_at", nullable = false)
	private Instant consentedAt;

	@Column(name = "unsubscribe_token", nullable = false, unique = true, length = 64)
	private String unsubscribeToken;

	@Column(name = "last_sent_at")
	private Instant lastSentAt;

	protected WeeklyEmailSubscription() {
	}

	WeeklyEmailSubscription(String studentLogin) {
		this.studentLogin = studentLogin;
	}

	/** Records (or renews) the consent; the next weekly run sends again. */
	void consent(String email, String repositoryFullName, String token, Instant now) {
		this.email = email;
		this.repositoryFullName = repositoryFullName;
		this.unsubscribeToken = token;
		this.consentedAt = now;
		this.lastSentAt = null;
	}

	void sentAt(Instant now) {
		this.lastSentAt = now;
	}

	String getStudentLogin() {
		return studentLogin;
	}

	String getEmail() {
		return email;
	}

	String getRepositoryFullName() {
		return repositoryFullName;
	}

	Instant getConsentedAt() {
		return consentedAt;
	}

	String getUnsubscribeToken() {
		return unsubscribeToken;
	}

	Instant getLastSentAt() {
		return lastSentAt;
	}

}
