package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.plan.Mission;

class WeeklyEmailTemplateTests {

	private static final String UNSUBSCRIBE = "https://vagaviva.dev/descadastrar?t=tok";

	private static final Mission MISSION = new Mission("patients-api", "Cadastro de pacientes", "Resumo", 2, true,
			List.of());

	@Test
	void missionEmailShowsMissionReviewAndUnsubscribe() {
		var reply = WeeklyEmailTemplate.mission("maria/vaga-viva-x", 2, MISSION, true, Optional.of("Releia o teste."),
				UNSUBSCRIBE);

		assertThat(reply.subject()).isEqualTo("Sua missão da semana: Cadastro de pacientes");
		assertThat(reply.text()).contains("https://github.com/maria/vaga-viva-x/issues", "Releia o teste.",
				UNSUBSCRIBE, "Cadastro de pacientes");
		assertThat(reply.html()).contains(UNSUBSCRIBE, "Releia o teste.");
	}

	@Test
	void htmlEscapesTheReview() {
		var reply = WeeklyEmailTemplate.mission("maria/vaga-viva-x", 1, MISSION, true,
				Optional.of("<script>x</script> **ok**"), UNSUBSCRIBE);

		assertThat(reply.html()).contains("&lt;script&gt;x&lt;/script&gt;").doesNotContain("<script>");
	}

	@Test
	void cutsLongReviewsAtAWord() {
		var review = "palavra ".repeat(100).strip();

		var reply = WeeklyEmailTemplate.mission("maria/vaga-viva-x", 1, MISSION, true, Optional.of(review),
				UNSUBSCRIBE);

		var shown = reply.text().lines().filter(l -> l.startsWith("palavra")).findFirst().orElseThrow();
		assertThat(shown).endsWith("palavra…");
		assertThat(shown.length()).isLessThanOrEqualTo(601);
		assertThat(shown.length()).isGreaterThan(560);
	}

	@Test
	void withoutTestsOrReview() {
		var reply = WeeklyEmailTemplate.mission("maria/vaga-viva-x", 1, MISSION, false, Optional.empty(),
				UNSUBSCRIBE);

		assertThat(reply.text()).contains("Ainda não vimos os testes rodarem no seu repositório.",
				"Você ainda não abriu um PR nesta missão.");
		assertThat(reply.html())
			.contains(org.springframework.web.util.HtmlUtils.htmlEscape("Ainda não vimos os testes rodarem no seu repositório."));
	}

	@Test
	void completedSubject() {
		var reply = WeeklyEmailTemplate.completed("maria/vaga-viva-x", UNSUBSCRIBE);

		assertThat(reply.subject()).isEqualTo("Você concluiu o projeto!");
		assertThat(reply.text()).contains(UNSUBSCRIBE, "https://github.com/maria/vaga-viva-x");
	}

}
