package io.github.precavidos.vagaviva.classroom;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends the Monday summary to every teacher who agreed to it. The log names the classroom id and the outcome,
 * never the teacher's address or the students (ADR 0003).
 */
class WeeklySummaryService {

	private static final Logger log = LoggerFactory.getLogger(WeeklySummaryService.class);

	/** Keeps the batch under Resend's request rate. */
	static final Duration PAUSE_BETWEEN_SENDS = Duration.ofMillis(600);

	private final ClassroomRepository classrooms;

	private final DashboardService dashboards;

	private final ClassroomMailer mailer;

	private final Clock clock;

	private final Pause pause;

	WeeklySummaryService(ClassroomRepository classrooms, DashboardService dashboards, ClassroomMailer mailer,
			Clock clock, Pause pause) {
		this.classrooms = classrooms;
		this.dashboards = dashboards;
		this.mailer = mailer;
		this.clock = clock;
		this.pause = pause;
	}

	record Result(int sent, int failed) {
	}

	/** Waits between sends; replaced in tests. */
	@FunctionalInterface
	interface Pause {

		Pause SLEEP = (duration) -> {
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

	/**
	 * One e-mail per consenting teacher and classroom. A failure for one classroom never stops the others. The
	 * idempotency key is per classroom and Monday, so a retried workflow run does not send twice.
	 */
	Result sendAll() {
		LocalDate monday = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC)
			.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		int sent = 0;
		int failed = 0;
		boolean attempted = false;
		for (Classroom classroom : classrooms.findByTeacherEmailIsNotNullAndTeacherEmailConsentAtIsNotNullOrderByCreatedAt()) {
			try {
				if (attempted) {
					pause.sleep(PAUSE_BETWEEN_SENDS);
				}
				attempted = true;
				WeeklySummary.Rendered summary = WeeklySummary.render(dashboards.dashboardForSummary(classroom),
						monday, mailer.link("/classrooms/" + classroom.getId()));
				mailer.send(classroom.getTeacherEmail(), summary.subject(), summary.text(), summary.html(),
						"classroom-summary/" + classroom.getId() + "/" + monday);
				sent++;
				log.info("Weekly summary sent for classroom {}", classroom.getId());
			}
			catch (RuntimeException e) {
				failed++;
				log.warn("Weekly summary failed for classroom {}: {}", classroom.getId(), e.getClass().getSimpleName());
			}
		}
		return new Result(sent, failed);
	}

}
