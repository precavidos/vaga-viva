package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;
import io.github.precavidos.vagaviva.email.WeeklyEmailService.Summary;
import io.github.precavidos.vagaviva.review.RepositorySnapshots;

/** The workflow reaches the endpoint with no session or CSRF token: only the shared secret counts. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "vagaviva.weekly-email.secret=monday-secret", "vagaviva.resend.api-key=re_test",
		"vagaviva.resend.from=Vaga Viva <vaga@vagaviva.dev>", "vagaviva.resend.public-url=https://vagaviva.dev" })
@AutoConfigureMockMvc
class WeeklyEmailTriggerApiTests {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	WeeklyEmailService service;

	@MockitoBean
	RepositorySnapshots snapshots;

	@Test
	void runsWithTheSecret() {
		when(snapshots.enabled()).thenReturn(true);
		when(service.sendDue()).thenReturn(new Summary(1, 0, 0));

		var response = mvc.post().uri("/api/weekly-emails").header("X-Vaga-Viva-Secret", "monday-secret").exchange();

		assertThat(response).hasStatus(HttpStatus.OK);
		assertThat(response).bodyJson().isLenientlyEqualTo("{\"sent\":1,\"skipped\":0,\"failed\":0}");
	}

	@Test
	void refusesAWrongSecret() {
		when(snapshots.enabled()).thenReturn(true);

		var response = mvc.post().uri("/api/weekly-emails").header("X-Vaga-Viva-Secret", "nope").exchange();

		assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(service, never()).sendDue();
	}

	@Test
	void refusesAMissingSecret() {
		when(snapshots.enabled()).thenReturn(true);

		var response = mvc.post().uri("/api/weekly-emails").exchange();

		assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
		verify(service, never()).sendDue();
	}

	@Test
	void isOffWithoutTheGitHubApp() {
		when(snapshots.enabled()).thenReturn(false);

		var response = mvc.post().uri("/api/weekly-emails").header("X-Vaga-Viva-Secret", "monday-secret").exchange();

		assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
		verifyNoInteractions(service);
	}

}
