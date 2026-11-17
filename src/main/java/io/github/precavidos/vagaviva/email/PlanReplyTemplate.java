package io.github.precavidos.vagaviva.email;

import java.util.Map;
import java.util.Optional;

import org.springframework.web.util.HtmlUtils;

import io.github.precavidos.vagaviva.archetype.ArchetypeFit;
import io.github.precavidos.vagaviva.jobposting.SeniorityLevel;
import io.github.precavidos.vagaviva.plan.ProjectPlan;

/** The texts, in Portuguese, of the e-mail that answers a forwarded job posting. */
final class PlanReplyTemplate {

	static final String FOOTER = "Você recebeu este e-mail porque encaminhou uma vaga para o Vaga Viva. "
			+ "Não guardamos o texto da vaga nem o seu endereço.";

	private static final String DEFAULT_SUBJECT = "Seu plano no Vaga Viva";

	private static final Map<SeniorityLevel, String> LEVELS = Map.of(SeniorityLevel.INTERN, "estágio",
			SeniorityLevel.JUNIOR, "júnior", SeniorityLevel.MID, "pleno", SeniorityLevel.SENIOR, "sênior");

	record Reply(String subject, String text, String html) {
	}

	private PlanReplyTemplate() {
	}

	static Reply plan(ProjectPlan plan, String archetypeName, Optional<String> link, String publicUrl,
			String originalSubject) {
		var level = LEVELS.getOrDefault(plan.seniorityLevel(), plan.seniorityLevel().name());
		var summary = "%s · Nível %s · %s · %d missões".formatted(archetypeName, level, plan.domain().name(),
				plan.missions().size());
		var warning = fitWarning(plan.archetypeFit(), archetypeName);
		var closing = link.map(url -> "Abra o seu plano completo: " + url)
			.orElse("Essa vaga é longa demais para caber num link. Abra " + publicUrl + " e cole o texto.");

		var text = new StringBuilder(summary).append("\n\n");
		var html = new StringBuilder("<p>").append(HtmlUtils.htmlEscape(summary)).append("</p>\n");
		if (!warning.isEmpty()) {
			text.append(warning).append("\n\n");
			html.append("<p>").append(HtmlUtils.htmlEscape(warning)).append("</p>\n");
		}
		html.append("<ol>\n");
		var number = 1;
		for (var planned : plan.missions()) {
			var mission = planned.mission();
			text.append(number).append(". ").append(mission.title()).append("\n   ").append(mission.summary())
				.append("\n");
			html.append("<li><strong>")
				.append(HtmlUtils.htmlEscape(mission.title()))
				.append("</strong><br>")
				.append(HtmlUtils.htmlEscape(mission.summary()))
				.append("</li>\n");
			number++;
		}
		html.append("</ol>\n");
		text.append("\n").append(closing).append("\n\n").append(FOOTER);
		if (link.isPresent()) {
			var url = HtmlUtils.htmlEscape(link.get());
			html.append("<p><a href=\"").append(url).append("\">Abra o seu plano completo</a></p>\n");
		}
		else {
			html.append("<p>").append(HtmlUtils.htmlEscape(closing)).append("</p>\n");
		}
		html.append("<p><small>").append(HtmlUtils.htmlEscape(FOOTER)).append("</small></p>");
		return new Reply(subject(originalSubject), text.toString(), html.toString());
	}

	static Reply help(String publicUrl, String originalSubject) {
		var text = "Não conseguimos ler uma vaga nesse e-mail. Cole o texto da vaga no corpo do e-mail "
				+ "e encaminhe de novo, ou abra " + publicUrl + " e cole o texto lá.\n\n" + FOOTER;
		var html = "<p>" + HtmlUtils.htmlEscape(
				"Não conseguimos ler uma vaga nesse e-mail. Cole o texto da vaga no corpo do e-mail "
						+ "e encaminhe de novo, ou abra " + publicUrl + " e cole o texto lá.")
				+ "</p>\n<p><small>" + HtmlUtils.htmlEscape(FOOTER) + "</small></p>";
		return new Reply(subject(originalSubject), text, html);
	}

	/** The same texts as {@code fitWarning} in the page; empty when the stack is covered or not stated. */
	static String fitWarning(ArchetypeFit fit, String stack) {
		if (fit == null || fit.status() == ArchetypeFit.Status.COVERED
				|| fit.status() == ArchetypeFit.Status.NOT_STATED) {
			return "";
		}
		var missing = String.join(", ", fit.uncovered());
		if (fit.status() == ArchetypeFit.Status.PARTIAL) {
			return "Este projeto usa " + stack + ". A vaga também cita " + missing + ", que o projeto não pratica.";
		}
		return "A vaga pede " + missing + ", e ainda não temos projeto para essa stack. "
				+ "Você pode criar o projeto em " + stack + " mesmo assim, para praticar o que vale nas duas.";
	}

	private static String subject(String original) {
		return original == null || original.isBlank() ? DEFAULT_SUBJECT : "Re: " + original.strip();
	}

}
