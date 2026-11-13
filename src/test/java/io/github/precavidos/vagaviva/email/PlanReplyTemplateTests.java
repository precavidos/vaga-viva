package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.archetype.ArchetypeFit;
import io.github.precavidos.vagaviva.archetype.ArchetypeFit.Status;
import io.github.precavidos.vagaviva.jobposting.SeniorityLevel;
import io.github.precavidos.vagaviva.plan.FictionalDomain;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.plan.PlannedMission;
import io.github.precavidos.vagaviva.plan.ProjectPlan;

class PlanReplyTemplateTests {

	private static final String FOOTER = "Você recebeu este e-mail porque encaminhou uma vaga para o Vaga Viva. "
			+ "Não guardamos o texto da vaga nem o seu endereço.";

	private static final String URL = "https://vagaviva.dev";

	@Test
	void planReplyListsTheMissionsAndTheLink() {
		var reply = PlanReplyTemplate.plan(plan("Cadastro de pacientes"), "Java e Spring Boot",
				Optional.of(URL + "/#vaga=abc"), URL, "Fwd: Vaga Java");

		assertThat(reply.subject()).isEqualTo("Re: Fwd: Vaga Java");
		assertThat(reply.text()).contains("Java e Spring Boot", "Nível júnior", "Agendamento de clínica",
				"Cadastro de pacientes", "Persistência", URL + "/#vaga=abc");
		assertThat(reply.text()).endsWith(FOOTER);
		assertThat(reply.html()).contains("Cadastro de pacientes", URL + "/#vaga=abc");
	}

	@Test
	void subjectFallsBackWhenBlank() {
		var reply = PlanReplyTemplate.plan(plan("Cadastro de pacientes"), "Java e Spring Boot", Optional.empty(), URL,
				"");

		assertThat(reply.subject()).isEqualTo("Seu plano no Vaga Viva");
	}

	@Test
	void longPostingAsksToPaste() {
		var reply = PlanReplyTemplate.plan(plan("Cadastro de pacientes"), "Java e Spring Boot", Optional.empty(), URL,
				"Vaga");

		assertThat(reply.text())
			.contains("Essa vaga é longa demais para caber num link. Abra https://vagaviva.dev e cole o texto.");
	}

	@Test
	void htmlEscapesValues() {
		var reply = PlanReplyTemplate.plan(plan("Cadastro <b>de</b> pacientes"), "Java e Spring Boot",
				Optional.empty(), URL, "Vaga");

		assertThat(reply.html()).contains("&lt;b&gt;").doesNotContain("<b>");
	}

	@Test
	void fitWarningMatchesThePage() {
		var partial = new ArchetypeFit("java-spring", Status.PARTIAL, List.of("Java"), List.of("Kotlin", "Go"));
		var none = new ArchetypeFit("java-spring", Status.NOT_COVERED, List.of(), List.of("Rust"));

		assertThat(PlanReplyTemplate.fitWarning(partial, "Java e Spring Boot")).isEqualTo(
				"Este projeto usa Java e Spring Boot. A vaga também cita Kotlin, Go, que o projeto não pratica.");
		assertThat(PlanReplyTemplate.fitWarning(none, "Java e Spring Boot")).isEqualTo(
				"A vaga pede Rust, e ainda não temos projeto para essa stack. Você pode criar o projeto em Java e Spring Boot mesmo assim, para praticar o que vale nas duas.");
		assertThat(PlanReplyTemplate.fitWarning(new ArchetypeFit("java-spring", Status.COVERED, List.of("Java"),
				List.of()), "x")).isEmpty();
		assertThat(PlanReplyTemplate.fitWarning(new ArchetypeFit("java-spring", Status.NOT_STATED, List.of(),
				List.of()), "x")).isEmpty();
	}

	@Test
	void helpExplainsTheBodyAndLinksThePage() {
		var reply = PlanReplyTemplate.help(URL, "Oi");

		assertThat(reply.text()).contains("no corpo do e-mail", URL).endsWith(FOOTER);
		assertThat(reply.subject()).isEqualTo("Re: Oi");
	}

	private static ProjectPlan plan(String firstTitle) {
		var fit = new ArchetypeFit("java-spring", Status.COVERED, List.of("Java"), List.of());
		var domain = new FictionalDomain("clinic-scheduling", "Agendamento de clínica", "Uma clínica fictícia.");
		var missions = List.of(planned("patients-api", firstTitle), planned("persistence", "Persistência"));
		return new ProjectPlan("java-spring", fit, domain, SeniorityLevel.JUNIOR, 3, missions, List.of(),
				List.of());
	}

	private static PlannedMission planned(String id, String title) {
		return new PlannedMission(new Mission(id, title, "Resumo de " + id, 1, true, List.of()),
				PlannedMission.Reason.CORE, List.of());
	}

}
