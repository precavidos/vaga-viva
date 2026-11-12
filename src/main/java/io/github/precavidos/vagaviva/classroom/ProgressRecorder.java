package io.github.precavidos.vagaviva.classroom;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.precavidos.vagaviva.review.ReviewEvents;
import io.github.precavidos.vagaviva.review.ReviewTextFilter;
import io.github.precavidos.vagaviva.review.TestReport;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps each classroom student's progress up to date from the webhook events.
 * Progress is computed against the plan stored in the classroom, never the
 * repository's {@code plan.json}, which the student can edit. A failure here is
 * logged with the repository only and never reaches the reviewer or the hints.
 */
@Component
class ProgressRecorder implements ReviewEvents {

	static final int MAX_MESSAGE_LENGTH = 200;

	private static final Logger log = LoggerFactory.getLogger(ProgressRecorder.class);

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final EnrollmentRepository enrollments;

	private final EnrollmentProgressRepository progresses;

	private final Clock clock;

	private final TransactionTemplate transaction;

	ProgressRecorder(EnrollmentRepository enrollments, EnrollmentProgressRepository progresses, Clock clock,
			PlatformTransactionManager transactionManager) {
		this.enrollments = enrollments;
		this.progresses = progresses;
		this.clock = clock;
		this.transaction = new TransactionTemplate(transactionManager);
	}

	@Override
	public boolean tracks(String repositoryFullName) {
		try {
			return enrollments.findByRepositoryFullName(repositoryFullName).isPresent();
		}
		catch (RuntimeException e) {
			log.warn("Could not check whether {} is in a classroom", repositoryFullName);
			return false;
		}
	}

	@Override
	public void activity(String repositoryFullName) {
		update(repositoryFullName, (progress, enrollment) -> progress.setLastActivityAt(clock.instant()));
	}

	@Override
	public void testsRan(String repositoryFullName, Integer pullRequestNumber, TestReport report) {
		update(repositoryFullName, (progress, enrollment) -> {
			Instant now = clock.instant();
			StoredPlan plan = StoredPlan.fromJson(enrollment.getClassroom().getPlan());
			switch (MissionProgress.from(plan.missionIds(), report)) {
				case MissionProgress.Unknown unknown -> {
					// A partial report says nothing about where the student is.
				}
				case MissionProgress.Completed completed -> {
					progress.setCompleted(true);
					progress.setFailingTests("[]");
				}
				case MissionProgress.OnMission onMission -> {
					if (!onMission.missionId().equals(progress.getCurrentMissionId())) {
						progress.setCurrentMissionId(onMission.missionId());
						progress.setCurrentMissionSince(now);
						progress.setPullRequestsOnMission("[]");
					}
					progress.setCompleted(false);
					progress.setFailingTests(JSON.writeValueAsString(onMission.failing()
						.stream()
						.map(failed -> new StoredFailingTest(failed.testName(), safeMessage(failed.message())))
						.toList()));
					if (pullRequestNumber != null) {
						List<Integer> numbers = read(progress.getPullRequestsOnMission(), new TypeReference<>() {
						}, new ArrayList<Integer>());
						if (!numbers.contains(pullRequestNumber)) {
							numbers.add(pullRequestNumber);
						}
						progress.setPullRequestsOnMission(JSON.writeValueAsString(numbers));
						progress.setLastPullRequestUrl(
								"https://github.com/" + repositoryFullName + "/pull/" + pullRequestNumber);
					}
				}
			}
			progress.setLastActivityAt(now);
		});
	}

	@Override
	public void reviewerAsked(String repositoryFullName, int pullRequestNumber, String question) {
		update(repositoryFullName, (progress, enrollment) -> {
			if (ReviewTextFilter.isSafe(question)) {
				progress.setLastReviewerQuestion(question);
			}
		});
	}

	@Override
	public void hintGiven(String repositoryFullName, String missionId) {
		update(repositoryFullName, (progress, enrollment) -> {
			Map<String, Integer> hints = read(progress.getHintsByMission(), new TypeReference<>() {
			}, new LinkedHashMap<String, Integer>());
			hints.merge(missionId, 1, Integer::sum);
			progress.setHintsByMission(JSON.writeValueAsString(hints));
		});
	}

	private void update(String repositoryFullName, ProgressChange change) {
		try {
			transaction.executeWithoutResult(status -> {
				Optional<Enrollment> enrollment = enrollments.findByRepositoryFullName(repositoryFullName);
				if (enrollment.isEmpty()) {
					return;
				}
				progresses.findById(enrollment.get().getId()).ifPresent(progress -> {
					change.apply(progress, enrollment.get());
					progresses.save(progress);
				});
			});
		}
		catch (RuntimeException e) {
			log.warn("Could not record the progress of {}", repositoryFullName);
		}
	}

	private static String safeMessage(String message) {
		if (message == null) {
			return null;
		}
		String cut = message.length() > MAX_MESSAGE_LENGTH ? message.substring(0, MAX_MESSAGE_LENGTH) : message;
		return ReviewTextFilter.isSafe(cut) ? cut : null;
	}

	private static <T> T read(String json, TypeReference<T> type, T empty) {
		return json == null || json.isBlank() ? empty : JSON.readValue(json, type);
	}

	private record StoredFailingTest(String name, String message) {
	}

	@FunctionalInterface
	private interface ProgressChange {

		void apply(EnrollmentProgress progress, Enrollment enrollment);

	}

}
