package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import io.github.precavidos.vagaviva.classroom.ClassroomStandings;
import io.github.precavidos.vagaviva.classroom.ClassroomStandings.Standing;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.RepositorySnapshot;
import io.github.precavidos.vagaviva.review.RepositorySnapshots;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;
import io.github.precavidos.vagaviva.review.TestReport;

@ExtendWith(OutputCaptureExtension.class)
class WeeklyEmailServiceTests {

	private static final Instant NOW = Instant.parse("2026-11-16T11:00:00Z");

	private static final Mission FIRST = new Mission("setup", "Preparar o projeto", "x", 1, true, List.of());

	private static final Mission SECOND = new Mission("patients-api", "Cadastro de pacientes", "x", 2, true,
			List.of());

	private static final StudentPlan PLAN = new StudentPlan("java-spring", "clinic", List.of(FIRST, SECOND));

	private final WeeklyEmailSubscriptionRepository subscriptions = mock(WeeklyEmailSubscriptionRepository.class);

	private final RepositorySnapshots snapshots = mock(RepositorySnapshots.class);

	private final ClassroomStandings standings = mock(ClassroomStandings.class);

	private final ResendApi resend = mock(ResendApi.class);

	private final List<Duration> pauses = new ArrayList<>();

	private final java.util.Map<String, WeeklyEmailSubscription> rows = new java.util.HashMap<>();

	private WeeklyEmailService service;

	@BeforeEach
	void setUp() {
		var properties = new ResendProperties("key", "secret", "Vaga Viva <oi@vagaviva.dev>", "https://vagaviva.dev/",
				5, 80);
		service = new WeeklyEmailService(subscriptions, snapshots, standings, resend, properties,
				Clock.fixed(NOW, ZoneOffset.UTC), pauses::add);
		when(standings.forRepository(any())).thenReturn(Optional.empty());
		when(subscriptions.findById(any()))
			.thenAnswer(call -> Optional.ofNullable(rows.get(call.<String>getArgument(0))));
	}

	private WeeklyEmailSubscription subscription(String login) {
		var subscription = new WeeklyEmailSubscription(login);
		subscription.consent(login + "@example.com", login + "/vaga-viva-x", "tok", Instant.parse("2026-11-01T00:00:00Z"));
		rows.put(login, subscription);
		return subscription;
	}

	private static RepositorySnapshot onSecondMission() {
		var report = new TestReport(Set.of("setup", "patients-api"),
				List.of(new FailedTest("patients-api", "t", "m")));
		return new RepositorySnapshot(PLAN, Optional.of(report), Optional.of("Releia o teste."));
	}

	private OutgoingEmail sentEmail() {
		var captor = ArgumentCaptor.forClass(OutgoingEmail.class);
		verify(resend).send(captor.capture());
		return captor.getValue();
	}

	@Test
	void sendsToADueSubscriber() {
		var maria = subscription("maria");
		when(subscriptions.findDue(any())).thenReturn(List.of(maria));
		when(snapshots.take("maria/vaga-viva-x")).thenReturn(Optional.of(onSecondMission()));

		var summary = service.sendDue();

		assertThat(summary).isEqualTo(new WeeklyEmailService.Summary(1, 0, 0));
		var email = sentEmail();
		assertThat(email.to()).isEqualTo("maria@example.com");
		assertThat(email.subject()).isEqualTo("Sua missão da semana: Cadastro de pacientes");
		assertThat(email.text()).contains("https://vagaviva.dev/descadastrar?t=tok", "Releia o teste.");
		assertThat(email.headers()).containsEntry("List-Unsubscribe",
				"<https://vagaviva.dev/api/weekly-email/unsubscribe?t=tok>")
			.containsEntry("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
		assertThat(email.idempotencyKey()).isEqualTo("weekly/maria/2026-11-16");
		verify(subscriptions).markSent("maria", NOW);
		verify(subscriptions, never()).save(any());
	}

	@Test
	void skipsAStudentWhoUnsubscribedAfterTheListing() {
		var maria = subscription("maria");
		when(subscriptions.findDue(any())).thenReturn(List.of(maria));
		rows.remove("maria");

		assertThat(service.sendDue()).isEqualTo(new WeeklyEmailService.Summary(0, 1, 0));
		verify(resend, never()).send(any());
		verify(subscriptions, never()).markSent(any(), any());
		verify(subscriptions, never()).save(any());
	}

	@Test
	void nobodyDueSendsNothing() {
		when(subscriptions.findDue(any())).thenReturn(List.of());

		assertThat(service.sendDue()).isEqualTo(new WeeklyEmailService.Summary(0, 0, 0));
		verify(resend, never()).send(any());
	}

	@Test
	void askedForSixDaysAgo() {
		when(subscriptions.findDue(any())).thenReturn(List.of());

		service.sendDue();

		verify(subscriptions).findDue(NOW.minus(6, ChronoUnit.DAYS));
	}

	@Test
	void skipsAMissingRepository() {
		var maria = subscription("maria");
		when(subscriptions.findDue(any())).thenReturn(List.of(maria));
		when(snapshots.take(any())).thenReturn(Optional.empty());

		assertThat(service.sendDue()).isEqualTo(new WeeklyEmailService.Summary(0, 1, 0));
		verify(resend, never()).send(any());
		verify(subscriptions, never()).markSent(any(), any());
	}

	@Test
	void congratulatesAndStopsWhenCompleted() {
		var maria = subscription("maria");
		when(subscriptions.findDue(any())).thenReturn(List.of(maria));
		var report = new TestReport(Set.of("setup", "patients-api"), List.of());
		when(snapshots.take(any())).thenReturn(Optional.of(new RepositorySnapshot(PLAN, Optional.of(report), Optional.empty())));

		assertThat(service.sendDue()).isEqualTo(new WeeklyEmailService.Summary(1, 0, 0));
		assertThat(sentEmail().subject()).isEqualTo("Você concluiu o projeto!");
		verify(subscriptions).removeByLogin("maria");
	}

	@Test
	void classroomStudentsUseTheirStanding() {
		var maria = subscription("maria");
		when(subscriptions.findDue(any())).thenReturn(List.of(maria));
		when(snapshots.take(any())).thenReturn(Optional.of(new RepositorySnapshot(PLAN, Optional.empty(), Optional.empty())));
		when(standings.forRepository("maria/vaga-viva-x")).thenReturn(Optional.of(new Standing("patients-api", false)));

		service.sendDue();

		assertThat(sentEmail().subject()).isEqualTo("Sua missão da semana: Cadastro de pacientes");
	}

	@Test
	void classroomStudentsWhoCompletedGetTheCongratulations() {
		var maria = subscription("maria");
		when(subscriptions.findDue(any())).thenReturn(List.of(maria));
		when(snapshots.take(any())).thenReturn(Optional.of(new RepositorySnapshot(PLAN, Optional.empty(), Optional.empty())));
		when(standings.forRepository(any())).thenReturn(Optional.of(new Standing("patients-api", true)));

		service.sendDue();

		assertThat(sentEmail().subject()).isEqualTo("Você concluiu o projeto!");
		verify(subscriptions).removeByLogin("maria");
	}

	@Test
	void unknownProgressFallsBackToTheFirstMission() {
		var maria = subscription("maria");
		when(subscriptions.findDue(any())).thenReturn(List.of(maria));
		var empty = new TestReport(Set.of(), List.of());
		when(snapshots.take(any())).thenReturn(Optional.of(new RepositorySnapshot(PLAN, Optional.of(empty), Optional.empty())));

		service.sendDue();

		var email = sentEmail();
		assertThat(email.subject()).isEqualTo("Sua missão da semana: Preparar o projeto");
		assertThat(email.text()).contains("Ainda não vimos os testes rodarem no seu repositório.");
	}

	@Test
	void oneFailureDoesNotStopTheBatch() {
		var maria = subscription("maria");
		var joao = subscription("joao");
		when(subscriptions.findDue(any())).thenReturn(List.of(maria, joao));
		when(snapshots.take(any())).thenReturn(Optional.of(onSecondMission()));
		org.mockito.Mockito.doThrow(new ResendApiException(500))
			.doNothing()
			.when(resend)
			.send(any());

		assertThat(service.sendDue()).isEqualTo(new WeeklyEmailService.Summary(1, 0, 1));
		verify(subscriptions, never()).markSent("maria", NOW);
		verify(subscriptions).markSent("joao", NOW);
		assertThat(pauses).containsExactly(Duration.ofMillis(600));
	}

	@Test
	void logsNoEmailAddress(CapturedOutput output) {
		var maria = subscription("maria");
		when(subscriptions.findDue(any())).thenReturn(List.of(maria));
		when(snapshots.take(any())).thenReturn(Optional.of(onSecondMission()));
		org.mockito.Mockito.doThrow(new ResendApiException(500)).when(resend).send(any());

		service.sendDue();

		assertThat(output.getAll()).doesNotContain("maria@example.com").contains("maria", "ResendApiException");
	}

}
