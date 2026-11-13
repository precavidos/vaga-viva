package io.github.precavidos.vagaviva.email;

import java.util.Optional;

import org.springframework.web.util.HtmlUtils;

import io.github.precavidos.vagaviva.plan.Mission;

/** The texts, in Portuguese, of the weekly e-mail. */
final class WeeklyEmailTemplate {

	static final int REVIEW_LIMIT = 600;

	private static final String NO_TESTS = "Ainda não vimos os testes rodarem no seu repositório.";

	private static final String NO_PULL_REQUEST = "Você ainda não abriu um PR nesta missão.";

	private WeeklyEmailTemplate() {
	}

	static PlanReplyTemplate.Reply mission(String repositoryFullName, int order, Mission mission, boolean testsSeen,
			Optional<String> lastReview, String unsubscribeUrl) {
		var issues = "https://github.com/" + repositoryFullName + "/issues";
		var heading = "Missão %d: %s".formatted(order, mission.title());
		var tests = testsSeen ? "" : NO_TESTS;
		var review = lastReview.map(WeeklyEmailTemplate::cut).filter(r -> !r.isBlank());
		var reviewHeading = "O último retorno do revisor:";
		var linkLine = "A descrição da missão está nas issues do seu repositório: " + issues;
		var footer = "Para parar de receber este e-mail: " + unsubscribeUrl;

		var text = new StringBuilder(heading).append("\n\n");
		var html = new StringBuilder("<p><strong>").append(HtmlUtils.htmlEscape(heading)).append("</strong></p>\n");
		if (!tests.isEmpty()) {
			text.append(tests).append("\n\n");
			html.append("<p>").append(HtmlUtils.htmlEscape(tests)).append("</p>\n");
		}
		if (review.isPresent()) {
			text.append(reviewHeading).append("\n").append(review.get()).append("\n\n");
			html.append("<p>").append(HtmlUtils.htmlEscape(reviewHeading)).append("<br>")
				.append(HtmlUtils.htmlEscape(review.get()))
				.append("</p>\n");
		}
		else {
			text.append(NO_PULL_REQUEST).append("\n\n");
			html.append("<p>").append(HtmlUtils.htmlEscape(NO_PULL_REQUEST)).append("</p>\n");
		}
		text.append(linkLine).append("\n\n").append(footer);
		html.append("<p>A descrição da missão está nas <a href=\"")
			.append(HtmlUtils.htmlEscape(issues))
			.append("\">issues do seu repositório</a>.</p>\n");
		html.append("<p><small><a href=\"")
			.append(HtmlUtils.htmlEscape(unsubscribeUrl))
			.append("\">Parar de receber este e-mail</a></small></p>");
		return new PlanReplyTemplate.Reply("Sua missão da semana: " + mission.title(), text.toString(),
				html.toString());
	}

	static PlanReplyTemplate.Reply completed(String repositoryFullName, String unsubscribeUrl) {
		var repository = "https://github.com/" + repositoryFullName;
		var body = "Todos os testes do seu projeto passam. Você concluiu todas as missões. Parabéns!";
		var linkLine = "Seu repositório: " + repository;
		var footer = "Este foi o último e-mail semanal, não precisa fazer nada. "
				+ "Para parar de receber e-mails do Vaga Viva: " + unsubscribeUrl;
		var text = body + "\n\n" + linkLine + "\n\n" + footer;
		var html = "<p>" + HtmlUtils.htmlEscape(body) + "</p>\n<p><a href=\"" + HtmlUtils.htmlEscape(repository)
				+ "\">Seu repositório</a></p>\n<p><small>Este foi o último e-mail semanal, não precisa fazer nada. "
				+ "<a href=\"" + HtmlUtils.htmlEscape(unsubscribeUrl) + "\">Parar de receber e-mails</a></small></p>";
		return new PlanReplyTemplate.Reply("Você concluiu o projeto!", text, html);
	}

	/** At most {@value #REVIEW_LIMIT} characters, never breaking a word, with an ellipsis when cut. */
	private static String cut(String review) {
		var text = review.strip();
		if (text.length() <= REVIEW_LIMIT) {
			return text;
		}
		var end = REVIEW_LIMIT;
		if (!Character.isWhitespace(text.charAt(end))) {
			while (end > 0 && !Character.isWhitespace(text.charAt(end - 1))) {
				end--;
			}
			if (end == 0) {
				end = REVIEW_LIMIT;
			}
		}
		return text.substring(0, end).stripTrailing() + "…";
	}

}
