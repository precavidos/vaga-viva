package io.github.precavidos.vagaviva.email;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.precavidos.vagaviva.classroom.ClassroomStandings;
import io.github.precavidos.vagaviva.classroom.MissionProgress;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.RepositorySnapshot;
import io.github.precavidos.vagaviva.review.RepositorySnapshots;

/**
 * Sends the weekly mission e-mail to every subscriber who is due. The log names the GitHub login and the outcome,
 * never the address (ADR 0003).
 */
public class WeeklyEmailService {

	private static final Logger log = LoggerFactory.getLogger(WeeklyEmailService.class);

	private static final Duration PAUSE_BETWEEN_SENDS = Duration.ofMillis(600);

	private final WeeklyEmailSubscriptionRepository subscriptions;

	private final RepositorySnapshots snapshots;

	private final ClassroomStandings standings;

	private final ResendApi resend;

	private final ResendProperties properties;

	private final Clock clock;

	private final Pause pause;

	WeeklyEmailService(WeeklyEmailSubscriptionRepository subscriptions, RepositorySnapshots snapshots,
			ClassroomStandings standings, ResendApi resend, ResendProperties properties, Clock clock, Pause pause) {
		this.subscriptions = subscriptions;
		this.snapshots = snapshots;
		this.standings = standings;
		this.resend = resend;
		this.properties = properties;
		this.clock = clock;
		this.pause = pause;
	}

	public record Summary(int sent, int skipped, int failed) {
	}

	/** Waits between sends; replaced in tests. */
	@FunctionalInterface
	interface Pause {

		Pause SLEEP = duration -> {
			try {
				Thread.sleep(duration);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Interrupted while waiting between e-mails", e);
			}
		};

		void sleep(Duration duration);

	}

	/** Not transactional as a whole: each student saves their own row right after the send. */
	public Summary sendDue() {
		var now = clock.instant();
		var due = subscriptions.findDue(now.minus(6, ChronoUnit.DAYS));
		int sent = 0;
		int skipped = 0;
		int failed = 0;
		boolean attempted = false;
		for (var listed : due) {
			var login = listed.getStudentLogin();
			try {
				// The batch takes minutes: a student may have unsubscribed since findDue.
				var current = subscriptions.findById(login);
				if (current.isEmpty()) {
					skipped++;
					log.info("Weekly e-mail skipped for {}: subscription removed", login);
					continue;
				}
				var subscription = current.get();
				var snapshot = snapshots.take(subscription.getRepositoryFullName());
				if (snapshot.isEmpty() || snapshot.get().plan().missions().isEmpty()) {
					skipped++;
					log.info("Weekly e-mail skipped for {}: no repository snapshot", login);
					continue;
				}
				if (attempted) {
					pause.sleep(PAUSE_BETWEEN_SENDS);
				}
				attempted = true;
				send(subscription, snapshot.get());
				sent++;
				log.info("Weekly e-mail sent for {}", login);
			}
			catch (RuntimeException e) {
				failed++;
				log.warn("Weekly e-mail failed for {}: {}", login, e.getClass().getSimpleName());
			}
		}
		return new Summary(sent, skipped, failed);
	}

	private void send(WeeklyEmailSubscription subscription, RepositorySnapshot snapshot) {
		var now = clock.instant();
		var repository = subscription.getRepositoryFullName();
		var token = subscription.getUnsubscribeToken();
		var publicUrl = properties.publicUrl();
		var footerUrl = publicUrl + "/descadastrar?t=" + token;
		var headerUrl = publicUrl + "/api/weekly-email/unsubscribe?t=" + token;
		var missions = snapshot.plan().missions();

		var position = position(repository, missions, snapshot);
		var reply = position.isEmpty() ? WeeklyEmailTemplate.completed(repository, footerUrl)
				: WeeklyEmailTemplate.mission(repository, missions.indexOf(position.get()) + 1, position.get(),
						testsSeen(snapshot), snapshot.lastReview(), footerUrl);

		var monday = LocalDate.ofInstant(now, ZoneOffset.UTC).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		var headers = Map.of("List-Unsubscribe", "<" + headerUrl + ">", "List-Unsubscribe-Post",
				"List-Unsubscribe=One-Click");
		resend.send(new OutgoingEmail(subscription.getEmail(), reply.subject(), reply.text(), reply.html(), headers,
				"weekly/" + subscription.getStudentLogin() + "/" + monday));

		if (position.isEmpty()) {
			subscriptions.removeByLogin(subscription.getStudentLogin());
		}
		else {
			subscriptions.markSent(subscription.getStudentLogin(), now);
		}
	}

	private static boolean testsSeen(RepositorySnapshot snapshot) {
		return snapshot.report().map(report -> !report.missionsRun().isEmpty()).orElse(false);
	}

	/** The mission the student is on, or empty when the whole plan is done. */
	private Optional<Mission> position(String repository, List<Mission> missions, RepositorySnapshot snapshot) {
		var standing = standings.forRepository(repository);
		if (standing.isPresent()) {
			if (standing.get().completed()) {
				return Optional.empty();
			}
			var current = missions.stream().filter(m -> m.id().equals(standing.get().currentMissionId())).findFirst();
			if (current.isPresent()) {
				return current;
			}
		}
		else if (snapshot.report().isPresent()) {
			var progress = MissionProgress.from(missions.stream().map(Mission::id).toList(), snapshot.report().get());
			if (progress instanceof MissionProgress.Completed) {
				return Optional.empty();
			}
			if (progress instanceof MissionProgress.OnMission onMission) {
				var current = missions.stream().filter(m -> m.id().equals(onMission.missionId())).findFirst();
				if (current.isPresent()) {
					return current;
				}
			}
		}
		return Optional.of(missions.get(0));
	}

}
