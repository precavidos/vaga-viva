package io.github.precavidos.vagaviva.classroom;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.web.util.HtmlUtils;

import io.github.precavidos.vagaviva.classroom.DashboardService.Card;
import io.github.precavidos.vagaviva.classroom.DashboardService.Column;
import io.github.precavidos.vagaviva.classroom.DashboardService.Counts;
import io.github.precavidos.vagaviva.classroom.DashboardService.Dashboard;

/**
 * The teacher's Monday e-mail, in Portuguese: the dashboard's counts, who is stuck or idle and on which mission,
 * and a link to the dashboard. It names students and missions only, never code. The HTML follows
 * {@code docs/design/prototipos/email.html}: tables, inline styles and system fonts, as e-mail clients require.
 */
final class WeeklySummary {

	static final String NOBODY_STUCK = "Ninguém travado ou parado nesta semana.";

	static final String NOBODY_ENROLLED = "Ainda ninguém entrou na turma. O link do convite está no painel.";

	private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

	private static final String STUCK_COLOR = "#ff6b4a";

	private static final String IDLE_COLOR = "#8b8678";

	private static final String ADVANCING_COLOR = "#33c47a";

	private WeeklySummary() {
	}

	record Rendered(String subject, String text, String html) {

		@Override
		public String toString() {
			return "Rendered[<redacted>]";
		}

	}

	static Rendered render(Dashboard dashboard, LocalDate weekOf, String dashboardUrl) {
		String week = weekOf.format(DAY_MONTH);
		String eyebrow = "Resumo da semana de " + week;
		Counts counts = dashboard.counts();
		int total = counts.stuck() + counts.idle() + counts.advancing() + counts.completed();
		List<Card> attention = attention(dashboard.columns());
		List<String> completed = completedNames(dashboard.columns());
		String footer = "Você recebe este resumo toda segunda porque pediu ao criar a turma “" + dashboard.name()
				+ "” no Vaga Viva. Os avisos são automáticos e servem para começar uma conversa, não para avaliar.";

		StringBuilder text = new StringBuilder(eyebrow).append(": ").append(dashboard.name()).append("\n\n");
		if (total == 0) {
			text.append(NOBODY_ENROLLED).append("\n\n");
		}
		else {
			text.append(countLine(counts)).append("\n\n");
			if (attention.isEmpty()) {
				text.append(NOBODY_STUCK).append("\n\n");
			}
			else {
				text.append("Vale uma conversa:\n");
				for (Card card : attention) {
					text.append("- ").append(card.displayName()).append(", ").append(statusWord(card)).append(": ")
						.append(detail(card)).append("\n");
				}
				text.append("\n");
			}
			if (!completed.isEmpty()) {
				text.append(completedLine(completed)).append("\n\n");
			}
		}
		text.append("Abrir o painel: ").append(dashboardUrl).append("\n\n").append(footer);

		return new Rendered(dashboard.name() + " — semana de " + week, text.toString(),
				html(dashboard, eyebrow, counts, total, attention, completed, dashboardUrl, footer));
	}

	/** Stuck students first, then idle ones, each group in the dashboard's mission order. */
	private static List<Card> attention(List<Column> columns) {
		List<Card> cards = columns.stream().flatMap((column) -> column.students().stream()).toList();
		return Stream
			.concat(cards.stream().filter((card) -> card.status() == StudentStatus.STUCK),
					cards.stream().filter((card) -> card.status() == StudentStatus.IDLE))
			.toList();
	}

	private static List<String> completedNames(List<Column> columns) {
		return columns.stream()
			.flatMap((column) -> column.students().stream())
			.filter((card) -> card.status() == StudentStatus.COMPLETED)
			.map(Card::displayName)
			.toList();
	}

	private static String countLine(Counts counts) {
		return String.join(" · ", plural(counts.stuck(), "travado", "travados"),
				plural(counts.idle(), "parado", "parados"), counts.advancing() + " avançando",
				plural(counts.completed(), "concluído", "concluídos"));
	}

	private static String plural(int count, String one, String many) {
		return count + " " + (count == 1 ? one : many);
	}

	private static String statusWord(Card card) {
		return card.status() == StudentStatus.STUCK ? "travado" : "parado";
	}

	/** "Missão 2, Persistência. 4 dias na missão 2, 3 PRs nesse período." */
	private static String detail(Card card) {
		String reason = capitalized(card.reason()) + ".";
		return card.missionOrder() < 1 ? reason
				: "Missão " + card.missionOrder() + ", " + card.missionTitle() + ". " + reason;
	}

	private static String completedLine(List<String> names) {
		String joined = names.size() == 1 ? names.get(0)
				: String.join(", ", names.subList(0, names.size() - 1)) + " e " + names.get(names.size() - 1);
		return joined + (names.size() == 1 ? " concluiu" : " concluíram") + " todas as missões.";
	}

	private static String capitalized(String text) {
		return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
	}

	private static String html(Dashboard dashboard, String eyebrow, Counts counts, int total, List<Card> attention,
			List<String> completed, String dashboardUrl, String footer) {
		String name = escape(dashboard.name());
		StringBuilder html = new StringBuilder();
		html.append("<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\">")
			.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
			.append("<meta name=\"color-scheme\" content=\"light\"><title>")
			.append(name)
			.append("</title></head>")
			.append("<body style=\"margin:0;padding:0;background:#f4f1ea;color:#141414;font-family:Arial,Helvetica,sans-serif;\">");
		html.append("<div style=\"display:none;max-height:0;overflow:hidden;\">")
			.append(escape(total == 0 ? NOBODY_ENROLLED : countLine(counts)))
			.append("</div>");
		html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f4f1ea;\">")
			.append("<tr><td align=\"center\" style=\"padding:32px 16px;\">")
			.append("<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:600px;\">")
			.append("<tr><td style=\"padding:0 4px 20px;font-size:20px;font-weight:bold;\">Vaga Viva</td></tr>")
			.append("<tr><td style=\"background:#ffffff;border:1px solid #dcd7ca;border-radius:12px;padding:36px 32px;\">");
		html.append("<p style=\"margin:0 0 8px;font-size:12px;letter-spacing:1px;text-transform:uppercase;color:#4a4740;font-family:'Courier New',monospace;font-weight:bold;\">")
			.append(escape(eyebrow))
			.append("</p>")
			.append("<h1 style=\"margin:0 0 24px;font-size:34px;line-height:1.1;\">")
			.append(name)
			.append("</h1>");

		if (total == 0) {
			html.append(paragraph(NOBODY_ENROLLED));
		}
		else {
			html.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>")
				.append(tile(counts.stuck(), counts.stuck() == 1 ? "travado" : "travados", STUCK_COLOR, "0 6px 0 0"))
				.append(tile(counts.idle(), counts.idle() == 1 ? "parado" : "parados", IDLE_COLOR, "0 3px"))
				.append(tile(counts.advancing(), "avançando", ADVANCING_COLOR, "0 0 0 6px"))
				.append("</tr></table>");
			if (attention.isEmpty()) {
				html.append(paragraph(NOBODY_STUCK));
			}
			else {
				html.append("<h2 style=\"margin:32px 0 12px;font-size:20px;\">Vale uma conversa</h2>")
					.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-size:16px;line-height:1.45;\">");
				for (int i = 0; i < attention.size(); i++) {
					Card card = attention.get(i);
					if (i > 0) {
						html.append("<tr><td style=\"height:8px;\"></td></tr>");
					}
					html.append("<tr><td style=\"padding:14px 16px;background:#f4f1ea;border-radius:8px;border-left:6px solid ")
						.append(card.status() == StudentStatus.STUCK ? STUCK_COLOR : IDLE_COLOR)
						.append(";\"><b>")
						.append(escape(card.displayName()))
						.append("</b> &nbsp;<span style=\"font-family:'Courier New',monospace;font-size:12px;font-weight:bold;letter-spacing:.5px;\">")
						.append(statusWord(card).toUpperCase(Locale.ROOT))
						.append("</span><br><span style=\"color:#4a4740;\">")
						.append(escape(detail(card)))
						.append("</span></td></tr>");
				}
				html.append("</table>");
			}
			if (!completed.isEmpty()) {
				html.append(paragraph(completedLine(completed)));
			}
		}

		html.append("<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:28px;\"><tr><td style=\"background:#5b3df5;border-radius:6px;\">")
			.append("<a href=\"")
			.append(escape(dashboardUrl))
			.append("\" style=\"display:inline-block;padding:15px 26px;color:#ffffff;font-size:16px;font-weight:bold;text-decoration:none;\">Abrir o painel &rarr;</a>")
			.append("</td></tr></table>")
			.append("</td></tr>")
			.append("<tr><td style=\"padding:20px 8px 0;font-size:13px;line-height:1.5;color:#4a4740;\">")
			.append(escape(footer))
			.append("</td></tr></table></td></tr></table></body></html>");
		return html.toString();
	}

	private static String tile(int count, String label, String color, String padding) {
		return "<td width=\"33%\" style=\"padding:" + padding + ";\">"
				+ "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>"
				+ "<td style=\"background:#f4f1ea;border-radius:8px;border-left:6px solid " + color
				+ ";padding:16px 16px 16px 10px;color:#141414;\">"
				+ "<div style=\"font-size:36px;font-weight:bold;line-height:1;\">" + count + "</div>"
				+ "<div style=\"font-size:14px;font-weight:bold;padding-top:6px;\">" + label + "</div>"
				+ "</td></tr></table></td>";
	}

	private static String paragraph(String text) {
		return "<p style=\"margin:20px 0 0;font-size:16px;line-height:1.45;color:#4a4740;\">" + escape(text) + "</p>";
	}

	private static String escape(String text) {
		// Only the markup characters: accents stay as UTF-8 characters.
		return HtmlUtils.htmlEscape(text, "UTF-8");
	}

}
