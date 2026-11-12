package io.github.precavidos.vagaviva.classroom;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Where a student is in the classroom plan, updated on each event. Shares its id with the
 * {@link Enrollment}. The JSON columns are kept as text and parsed by the services.
 */
@Entity
@Table(name = "enrollment_progress")
class EnrollmentProgress {

	@Id
	@Column(name = "enrollment_id")
	private UUID id;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "enrollment_id")
	private Enrollment enrollment;

	@Column(name = "current_mission_id")
	private String currentMissionId;

	@Column(name = "current_mission_since")
	private Instant currentMissionSince;

	@Column(nullable = false)
	private boolean completed;

	@Column(name = "last_activity_at")
	private Instant lastActivityAt;

	/** JSON of {@code [{name, message}]}. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "failing_tests", columnDefinition = "jsonb")
	private String failingTests;

	/** JSON of {@code [number]}. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "pull_requests_on_mission", columnDefinition = "jsonb")
	private String pullRequestsOnMission;

	@Column(name = "last_reviewer_question", columnDefinition = "text")
	private String lastReviewerQuestion;

	@Column(name = "last_pull_request_url")
	private String lastPullRequestUrl;

	/** JSON of {@code {missionId: number}}. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "hints_by_mission", columnDefinition = "jsonb")
	private String hintsByMission;

	protected EnrollmentProgress() {
	}

	EnrollmentProgress(Enrollment enrollment) {
		this.enrollment = enrollment;
	}

	UUID getId() {
		return id;
	}

	Enrollment getEnrollment() {
		return enrollment;
	}

	String getCurrentMissionId() {
		return currentMissionId;
	}

	void setCurrentMissionId(String currentMissionId) {
		this.currentMissionId = currentMissionId;
	}

	Instant getCurrentMissionSince() {
		return currentMissionSince;
	}

	void setCurrentMissionSince(Instant currentMissionSince) {
		this.currentMissionSince = currentMissionSince;
	}

	boolean isCompleted() {
		return completed;
	}

	void setCompleted(boolean completed) {
		this.completed = completed;
	}

	Instant getLastActivityAt() {
		return lastActivityAt;
	}

	void setLastActivityAt(Instant lastActivityAt) {
		this.lastActivityAt = lastActivityAt;
	}

	String getFailingTests() {
		return failingTests;
	}

	void setFailingTests(String failingTests) {
		this.failingTests = failingTests;
	}

	String getPullRequestsOnMission() {
		return pullRequestsOnMission;
	}

	void setPullRequestsOnMission(String pullRequestsOnMission) {
		this.pullRequestsOnMission = pullRequestsOnMission;
	}

	String getLastReviewerQuestion() {
		return lastReviewerQuestion;
	}

	void setLastReviewerQuestion(String lastReviewerQuestion) {
		this.lastReviewerQuestion = lastReviewerQuestion;
	}

	String getLastPullRequestUrl() {
		return lastPullRequestUrl;
	}

	void setLastPullRequestUrl(String lastPullRequestUrl) {
		this.lastPullRequestUrl = lastPullRequestUrl;
	}

	String getHintsByMission() {
		return hintsByMission;
	}

	void setHintsByMission(String hintsByMission) {
		this.hintsByMission = hintsByMission;
	}

}
