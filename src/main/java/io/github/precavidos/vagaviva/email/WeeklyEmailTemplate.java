package io.github.precavidos.vagaviva.email;

import java.util.Optional;

import org.springframework.web.util.HtmlUtils;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.watch.CompanyNews;
import io.github.precavidos.vagaviva.watch.CompanyWatch;

/** The texts, in Portuguese, of the weekly e-mail. */
final class WeeklyEmailTemplate {

	static final int REVIEW_LIMIT = 600;

	private static final String NO_TESTS = "Ainda não vimos os testes rodarem no seu repositório.";

	private static final String NO_PULL_REQUEST = "Você ainda não abriu um PR nesta missão.";

	private WeeklyEmailTemplate() {
	}

	static PlanReplyTemplate.Reply mission(String repositoryFullName, int order, Mission mission, boolean testsSeen,
			Optional<String> lastReview, String unsubscribeUrl) {
		return mission(repositoryFullName, order, mission, testsSeen, lastReview, unsubscribeUrl, Optional.empty(), "");
	}

	/** {@code companies} is empty while the company watch is off, and the e-mail stays as it was. */
	static PlanReplyTemplate.Reply mission(String repositoryFullName, int order, Mission mission, boolean testsSeen,
			Optional<String> lastReview, String unsubscribeUrl, Optional<CompanyNews.Section> companies,
			String publicUrl) {
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
				.append(HtmlUtils.htmlEscape(review.get()).replace("\n", "<br>"))
				.append("</p>\n");
		}
		else {
			text.append(NO_PULL_REQUEST).append("\n\n");
			html.append("<p>").append(HtmlUtils.htmlEscape(NO_PULL_REQUEST)).append("</p>\n");
		}
		text.append(linkLine).append("\n\n");
		html.append("<p>A descrição da missão está nas <a href=\"")
			.append(HtmlUtils.htmlEscape(issues))
			.append("\">issues do seu repositório</a>.</p>\n");
		companies.ifPresent(section -> companies(section, publicUrl, text, html));
		text.append(footer);
		html.append("<p><small><a href=\"")
			.append(HtmlUtils.htmlEscape(unsubscribeUrl))
			.append("\">Parar de receber este e-mail</a></small></p>");
		return new PlanReplyTemplate.Reply("Sua missão da semana: " + mission.title(), text.toString(),
				html.toString());
	}

	/** "Vagas novas nas empresas que você segue", and the link to the page where the student picks them. */
	private static void companies(CompanyNews.Section section, String publicUrl, StringBuilder text,
			StringBuilder html) {
		var followPage = publicUrl + "/companies";
		var followLink = "<a href=\"" + HtmlUtils.htmlEscape(followPage) + "\">";
		if (!section.following()) {
			var invite = "Siga até " + CompanyWatch.MAX_FOLLOWS + " empresas e receba aqui as vagas novas delas";
			text.append(invite).append(": ").append(followPage).append("\n\n");
			html.append("<p>").append(followLink).append(HtmlUtils.htmlEscape(invite)).append("</a>.</p>\n");
			return;
		}
		var heading = "Vagas novas nas empresas que você segue";
		text.append(heading).append(":\n\n");
		html.append("<p><strong>").append(HtmlUtils.htmlEscape(heading)).append("</strong></p>\n");
		if (section.postings().isEmpty()) {
			var none = "Nenhuma vaga nova de tecnologia nas empresas que você segue nesta semana.";
			text.append(none).append("\n\n");
			html.append("<p>").append(HtmlUtils.htmlEscape(none)).append("</p>\n");
		}
		for (var posting : section.postings()) {
			var match = posting.matchesPlan() ? " (combina com o seu plano)" : "";
			var plan = PostingLink.ofUrl(publicUrl, posting.url());
			text.append(posting.title()).append(" — ").append(posting.company()).append(match).append("\n")
				.append(posting.url()).append("\n")
				.append("Montar meu plano para esta vaga: ").append(plan).append("\n\n");
			html.append("<p><a href=\"").append(HtmlUtils.htmlEscape(posting.url())).append("\">")
				.append(HtmlUtils.htmlEscape(posting.title())).append("</a> — ")
				.append(HtmlUtils.htmlEscape(posting.company()));
			if (posting.matchesPlan()) {
				html.append(" <strong>(combina com o seu plano)</strong>");
			}
			html.append("<br><a href=\"").append(HtmlUtils.htmlEscape(plan))
				.append("\" style=\"display:inline-block;margin-top:6px;padding:8px 14px;background:#5b3df5;color:#ffffff;"
						+ "border-radius:6px;text-decoration:none\">Montar meu plano para esta vaga</a></p>\n");
		}
		for (var name : section.checkLinks()) {
			var check = "Não conseguimos ler a página de vagas de " + name + " nas últimas "
					+ CompanyWatch.FAILURES_TO_WARN + " semanas. Confira o link em ";
			text.append(check).append(followPage).append(".\n\n");
			html.append("<p>").append(HtmlUtils.htmlEscape(check)).append(followLink)
				.append(HtmlUtils.htmlEscape(followPage)).append("</a>.</p>\n");
		}
		text.append("Empresas que você segue: ").append(followPage).append("\n\n");
		html.append("<p>").append(followLink).append("Empresas que você segue</a></p>\n");
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
