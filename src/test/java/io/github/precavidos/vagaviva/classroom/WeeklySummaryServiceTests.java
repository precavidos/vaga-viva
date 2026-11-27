package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.classroom.DashboardService.Column;
import io.github.precavidos.vagaviva.classroom.DashboardService.Counts;
import io.github.precavidos.vagaviva.classroom.DashboardService.Dashboard;

class WeeklySummaryServiceTests {

	/** A Wednesday: the key still names that week's Monday. */
	private static final Instant NOW = Instant.parse("2026-11-25T11:00:00Z");

	private final ClassroomRepository classrooms = mock(ClassroomRepository.class);

	private final DashboardService dashboards = mock(DashboardService.class);

	private final ClassroomMailer mailer = mock(ClassroomMailer.class);

	private final List<Duration> pauses = new ArrayList<>();

	private final WeeklySummaryService service = new WeeklySummaryService(classrooms, dashboards, mailer,
			Clock.fixed(NOW, ZoneOffset.UTC), pauses::add);

	@Test
	void sendsOneSummaryPerClassroomWithAKeyPerMonday() {
		Classroom first = classroom("ana@escola.dev");
		Classroom second = classroom("bia@escola.dev");
		when(classrooms.findByTeacherEmailIsNotNullAndTeacherEmailConsentAtIsNotNullOrderByCreatedAt())
			.thenReturn(List.of(first, second));
		when(dashboards.dashboardForSummary(any())).thenAnswer((call) -> empty(call.getArgument(0)));
		when(mailer.link(anyString())).thenAnswer((call) -> "https://vagaviva.dev" + call.getArgument(0));

		var result = service.sendAll();

		assertThat(result).isEqualTo(new WeeklySummaryService.Result(2, 0));
		verify(mailer).send(eq("ana@escola.dev"), startsWith("Turma — semana de 23/11"), anyString(), anyString(),
				eq("classroom-summary/" + first.getId() + "/2026-11-23"));
		verify(mailer).link("/classrooms/" + second.getId());
		assertThat(pauses).containsExactly(WeeklySummaryService.PAUSE_BETWEEN_SENDS);
	}

	@Test
	void oneFailureDoesNotStopTheOthers() {
		Classroom broken = classroom("ana@escola.dev");
		Classroom fine = classroom("bia@escola.dev");
		when(classrooms.findByTeacherEmailIsNotNullAndTeacherEmailConsentAtIsNotNullOrderByCreatedAt())
			.thenReturn(List.of(broken, fine));
		when(dashboards.dashboardForSummary(any())).thenAnswer((call) -> empty(call.getArgument(0)));
		when(mailer.link(anyString())).thenReturn("https://vagaviva.dev/classrooms/x");
		doThrow(new IllegalStateException("Resend is down")).when(mailer)
			.send(eq("ana@escola.dev"), anyString(), anyString(), anyString(), anyString());

		var result = service.sendAll();

		assertThat(result).isEqualTo(new WeeklySummaryService.Result(1, 1));
		verify(mailer).send(eq("bia@escola.dev"), anyString(), anyString(), anyString(), anyString());
	}

	private static Classroom classroom(String email) {
		return new Classroom(UUID.randomUUID(), "code-" + email, "Turma", "david", email, NOW, "{}", NOW);
	}

	private static Dashboard empty(Classroom classroom) {
		return new Dashboard(classroom.getId(), classroom.getName(), "Java", "/join/x", new Counts(0, 0, 0, 0),
				List.of(new Column(null, null, "Concluído", List.of())));
	}

}
