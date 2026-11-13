package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

/** Without a webhook secret the endpoint is off. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "vagaviva.resend.api-key=re_test", "vagaviva.resend.webhook-secret=",
		"vagaviva.resend.from=Vaga Viva <vaga@vagaviva.dev>", "vagaviva.resend.public-url=https://vagaviva.dev" })
@AutoConfigureMockMvc
class InboundEmailDisabledApiTests {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	ResendApi resend;

	@Test
	void isOffWithoutConfiguration() {
		var response = mvc.post()
			.uri("/api/inbound-email")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"type\":\"email.received\"}")
			.exchange();

		assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
		verifyNoInteractions(resend);
	}

}
