package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;
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
import io.github.precavidos.vagaviva.review.RepositorySnapshots;

/** Without the weekly secret, or without the Resend sending settings, the trigger is off. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "vagaviva.weekly-email.secret=", "vagaviva.resend.api-key=re_test",
		"vagaviva.resend.from=Vaga Viva <vaga@vagaviva.dev>", "vagaviva.resend.public-url=https://vagaviva.dev" })
@AutoConfigureMockMvc
class WeeklyEmailTriggerDisabledApiTests {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	WeeklyEmailService service;

	@MockitoBean
	RepositorySnapshots snapshots;

	@Test
	void isOffWithoutConfiguration() {
		when(snapshots.enabled()).thenReturn(true);

		var response = mvc.post().uri("/api/weekly-emails").header("X-Vaga-Viva-Secret", "anything").exchange();

		assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
		verifyNoInteractions(service);
	}

}
