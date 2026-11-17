package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ResendClassroomMailerTests {

	private final ResendApi resend = mock(ResendApi.class);

	@Test
	void sendsThroughResendWithTheKey() {
		var mailer = new ResendClassroomMailer(resend, properties("re_test"));

		mailer.send("prof@escola.dev", "Turma — semana de 23/11", "texto", "<p>html</p>", "classroom-summary/x/2026-11-23");

		ArgumentCaptor<OutgoingEmail> sent = ArgumentCaptor.forClass(OutgoingEmail.class);
		verify(resend).send(sent.capture());
		assertThat(sent.getValue().to()).isEqualTo("prof@escola.dev");
		assertThat(sent.getValue().subject()).isEqualTo("Turma — semana de 23/11");
		assertThat(sent.getValue().idempotencyKey()).isEqualTo("classroom-summary/x/2026-11-23");
	}

	@Test
	void linksUseThePublicUrl() {
		assertThat(new ResendClassroomMailer(resend, properties("re_test")).link("/turmas/1"))
			.isEqualTo("https://vagaviva.dev/turmas/1");
	}

	@Test
	void isOffWithoutTheKey() {
		assertThat(new ResendClassroomMailer(resend, properties("")).enabled()).isFalse();
		assertThat(new ResendClassroomMailer(resend, properties("re_test")).enabled()).isTrue();
	}

	private static ResendProperties properties(String apiKey) {
		return new ResendProperties(apiKey, "", "Vaga Viva <vaga@vagaviva.dev>", "https://vagaviva.dev/", 5, 80);
	}

}
