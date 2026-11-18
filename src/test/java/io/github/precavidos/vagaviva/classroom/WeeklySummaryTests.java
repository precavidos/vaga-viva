package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.classroom.DashboardService.Card;
import io.github.precavidos.vagaviva.classroom.DashboardService.Column;
import io.github.precavidos.vagaviva.classroom.DashboardService.Counts;
import io.github.precavidos.vagaviva.classroom.DashboardService.Dashboard;

class WeeklySummaryTests {

	private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);

	private static final String URL = "https://vagaviva.dev/turmas/abc";

	@Test
	void listsStuckAndIdleStudentsWithTheirMission() {
		Dashboard dashboard = dashboard(new Counts(1, 1, 1, 1),
				List.of(column(1, "API de pacientes", card("Caio", StudentStatus.IDLE, 1, "API de pacientes",
						"sem atividade há 6 dias"), card("Bia", StudentStatus.ADVANCING, 1, "API de pacientes", "hoje")),
						column(2, "Persistência", card("João Pedro", StudentStatus.STUCK, 2, "Persistência",
								"4 dias na missão 2, 3 PRs nesse período")),
						done(card("Ana", StudentStatus.COMPLETED, 2, "Projeto concluído", "todas as missões passando"))));

		var summary = WeeklySummary.render(dashboard, MONDAY, URL);

		assertThat(summary.subject()).isEqualTo("Turma piloto — semana de 23/11");
		assertThat(summary.text()).contains("1 travado · 1 parado · 1 avançando · 1 concluído")
			.contains("- João Pedro, travado: Missão 2, Persistência. 4 dias na missão 2, 3 PRs nesse período.")
			.contains("- Caio, parado: Missão 1, API de pacientes. Sem atividade há 6 dias.")
			.contains("Ana concluiu todas as missões.")
			.contains("Abrir o painel: " + URL)
			.doesNotContain("Bia");
		// Stuck before idle, whatever the mission order.
		assertThat(summary.text().indexOf("João Pedro")).isLessThan(summary.text().indexOf("Caio"));
		assertThat(summary.html()).contains("href=\"" + URL + "\"").contains("TRAVADO").contains("PARADO");
	}

	@Test
	void saysWhenNobodyIsStuck() {
		Dashboard dashboard = dashboard(new Counts(0, 0, 2, 0), List.of(column(1, "API de pacientes",
				card("Bia", StudentStatus.ADVANCING, 1, "API de pacientes", "hoje"),
				card("Caio", StudentStatus.ADVANCING, 1, "API de pacientes", "há 1 dia na missão")), done()));

		var summary = WeeklySummary.render(dashboard, MONDAY, URL);

		assertThat(summary.text()).contains("0 travados · 0 parados · 2 avançando · 0 concluídos")
			.contains(WeeklySummary.NOBODY_STUCK)
			.doesNotContain("Vale uma conversa");
		assertThat(summary.html()).contains(WeeklySummary.NOBODY_STUCK);
	}

	@Test
	void saysWhenNobodyJoinedYet() {
		var summary = WeeklySummary.render(dashboard(new Counts(0, 0, 0, 0), List.of(column(1, "API de pacientes"), done())),
				MONDAY, URL);

		assertThat(summary.text()).contains(WeeklySummary.NOBODY_ENROLLED).doesNotContain(WeeklySummary.NOBODY_STUCK);
	}

	@Test
	void joinsSeveralNamesThatCompleted() {
		var summary = WeeklySummary.render(dashboard(new Counts(0, 0, 0, 3),
				List.of(done(card("Ana", StudentStatus.COMPLETED, 1, "Projeto concluído", "todas as missões passando"),
						card("Bia", StudentStatus.COMPLETED, 1, "Projeto concluído", "todas as missões passando"),
						card("Caio", StudentStatus.COMPLETED, 1, "Projeto concluído", "todas as missões passando")))),
				MONDAY, URL);

		assertThat(summary.text()).contains("Ana, Bia e Caio concluíram todas as missões.");
	}

	@Test
	void escapesNamesInHtml() {
		Dashboard dashboard = new Dashboard(UUID.randomUUID(), "<i>Turma</i>", "Java", "/turma/x",
				new Counts(1, 0, 0, 0), List.of(column(1, "API <x>", card("<b>Ana</b>", StudentStatus.STUCK, 1,
						"API <x>", "3 dias na missão 1, 0 PRs nesse período")), done()));

		var summary = WeeklySummary.render(dashboard, MONDAY, URL);

		assertThat(summary.html()).contains("&lt;b&gt;Ana&lt;/b&gt;")
			.contains("&lt;i&gt;Turma&lt;/i&gt;")
			.contains("API &lt;x&gt;")
			.doesNotContain("<b>Ana</b>")
			.doesNotContain("<i>Turma</i>");
	}

	private static Dashboard dashboard(Counts counts, List<Column> columns) {
		return new Dashboard(UUID.randomUUID(), "Turma piloto", "Java + Spring Boot", "/turma/abc", counts, columns);
	}

	private static Column column(int order, String title, Card... cards) {
		return new Column(order, "m" + order, title, List.of(cards));
	}

	private static Column done(Card... cards) {
		return new Column(null, null, "Concluído", List.of(cards));
	}

	private static Card card(String name, StudentStatus status, int order, String title, String reason) {
		return new Card(name.toLowerCase(), name, status, 0, order, title, reason, List.of());
	}

}
