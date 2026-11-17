package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

/** The workflow reaches the route with no session or CSRF token: only the shared secret counts. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "vagaviva.classroom.teachers=david", "vagaviva.classroom.summary-secret=monday-secret" })
@AutoConfigureMockMvc
class WeeklySummaryApiTests {

	private static final String POSTING = "Desenvolvedora Java Júnior com Spring Boot, PostgreSQL e Docker.";

	@Autowired
	MockMvcTester mvc;

	@Autowired
	ClassroomService classrooms;

	@MockitoBean
	ClassroomMailer mailer;

	@Test
	void sendsOnlyToTeachersWhoConsented() {
		when(mailer.enabled()).thenReturn(true);
		when(mailer.link(anyString())).thenAnswer((call) -> "https://vagaviva.dev" + call.getArgument(0));
		Classroom consented = classrooms.create("david", POSTING, "Turma com resumo", "prof@escola.dev", true);
		classrooms.create("david", POSTING, "Turma sem e-mail", null, false);

		var response = mvc.post()
			.uri("/api/classrooms/weekly-summaries")
			.header("X-Vaga-Viva-Secret", "monday-secret")
			.exchange();

		assertThat(response).hasStatusOk();
		// Other tests share the database, so count what was sent instead of assuming a number.
		ArgumentCaptor<String> recipients = ArgumentCaptor.forClass(String.class);
		verify(mailer, atLeastOnce()).send(recipients.capture(), anyString(), anyString(), anyString(), anyString());
		assertThat(recipients.getAllValues()).contains("prof@escola.dev").doesNotContainNull();
		assertThat(response).bodyJson()
			.isLenientlyEqualTo("{\"sent\":" + recipients.getAllValues().size() + ",\"failed\":0}");
		verify(mailer).send(eq("prof@escola.dev"), startsWith("Turma com resumo — semana de "), anyString(),
				anyString(), startsWith("classroom-summary/" + consented.getId() + "/"));
		verify(mailer, never()).send(anyString(), startsWith("Turma sem e-mail"), anyString(), anyString(),
				anyString());
	}

	@Test
	void wrongSecretIs401() {
		when(mailer.enabled()).thenReturn(true);

		var response = mvc.post()
			.uri("/api/classrooms/weekly-summaries")
			.header("X-Vaga-Viva-Secret", "nope")
			.exchange();

		assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(mailer, never()).send(anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void missingSecretIs401() {
		when(mailer.enabled()).thenReturn(true);

		var response = mvc.post().uri("/api/classrooms/weekly-summaries").exchange();

		assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void withoutEmailItIs503() {
		when(mailer.enabled()).thenReturn(false);

		var response = mvc.post()
			.uri("/api/classrooms/weekly-summaries")
			.header("X-Vaga-Viva-Secret", "monday-secret")
			.exchange();

		assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
		verify(mailer, never()).send(anyString(), anyString(), anyString(), anyString(), anyString());
	}

}
