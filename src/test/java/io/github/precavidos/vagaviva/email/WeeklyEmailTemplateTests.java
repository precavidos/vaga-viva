package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.watch.CompanyNews;

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

	private static final String PUBLIC_URL = "https://vagaviva.dev";

	private static final CompanyNews.Posting MATCHING = new CompanyNews.Posting("Dev Java <Júnior>", "Compass",
			"https://compass.gupy.io/jobs/1", true);

	private static final CompanyNews.Posting OTHER = new CompanyNews.Posting("Dev Python", "Compass",
			"https://compass.gupy.io/jobs/2", false);

	@Test
	void listsTheNewPostingsOfTheFollowedCompanies() {
		var reply = withCompanies(new CompanyNews.Section(List.of(MATCHING, OTHER), List.of(), true));

		assertThat(reply.text()).contains("Vagas novas nas empresas que você segue",
				"Dev Java <Júnior> — Compass (combina com o seu plano)", "https://compass.gupy.io/jobs/1",
				"Montar meu plano para esta vaga: https://vagaviva.dev/#link=https%3A%2F%2Fcompass.gupy.io%2Fjobs%2F1",
				"Dev Python — Compass\n", "Empresas que você segue: https://vagaviva.dev/empresas");
		assertThat(reply.text().indexOf("Dev Java")).isLessThan(reply.text().indexOf("Dev Python"));
		assertThat(reply.html()).contains(org.springframework.web.util.HtmlUtils.htmlEscape("Dev Java <Júnior>"),
				"combina com o seu plano",
				"href=\"https://vagaviva.dev/#link=https%3A%2F%2Fcompass.gupy.io%2Fjobs%2F1\"",
				"Montar meu plano para esta vaga", "href=\"https://vagaviva.dev/empresas\"")
			.doesNotContain("<Júnior>");
	}

	@Test
	void saysWhenTheFollowedCompaniesHaveNothingNew() {
		var reply = withCompanies(new CompanyNews.Section(List.of(), List.of(), true));

		assertThat(reply.text()).contains("Nenhuma vaga nova de tecnologia nas empresas que você segue nesta semana.");
	}

	@Test
	void invitesToFollowWhenTheStudentFollowsNobody() {
		var reply = withCompanies(new CompanyNews.Section(List.of(), List.of(), false));

		assertThat(reply.text()).contains("Siga até 3 empresas e receba aqui as vagas novas delas: https://vagaviva.dev/empresas")
			.doesNotContain("Vagas novas nas empresas que você segue");
	}

	@Test
	void asksToCheckTheLinkOfAPageThatKeepsFailing() {
		var reply = withCompanies(new CompanyNews.Section(List.of(), List.of("Compass"), true));

		assertThat(reply.text()).contains(
				"Não conseguimos ler a página de vagas de Compass nas últimas 3 semanas. Confira o link em https://vagaviva.dev/empresas.");
		assertThat(reply.html()).contains("Confira o link em <a href=\"https://vagaviva.dev/empresas\">");
	}

	@Test
	void staysAsItWasWhileTheWatchIsOff() {
		var before = WeeklyEmailTemplate.mission("maria/vaga-viva-x", 1, MISSION, true, Optional.empty(), UNSUBSCRIBE);
		var after = WeeklyEmailTemplate.mission("maria/vaga-viva-x", 1, MISSION, true, Optional.empty(), UNSUBSCRIBE,
				Optional.empty(), PUBLIC_URL);

		assertThat(after).isEqualTo(before);
		assertThat(after.text()).doesNotContain("empresas");
	}

	private static PlanReplyTemplate.Reply withCompanies(CompanyNews.Section section) {
		return WeeklyEmailTemplate.mission("maria/vaga-viva-x", 1, MISSION, true, Optional.empty(), UNSUBSCRIBE,
				Optional.of(section), PUBLIC_URL);
	}

}
