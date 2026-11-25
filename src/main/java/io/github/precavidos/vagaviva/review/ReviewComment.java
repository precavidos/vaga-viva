package io.github.precavidos.vagaviva.review;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds the reviewer's comment, in Portuguese, for students new to GitHub: titled blocks, a progress table of the
 * plan, the failing tests and numbered questions. The structure and every number come from the test report (ADR
 * 0002); the LLM only fills {@link Summary#working()} and {@link Summary#questions()}, each already checked by
 * {@link ReviewTextFilter}. Names and messages from the student's repository are neutralized before they are quoted.
 * <p>
 * The comment carries two hidden markers: the first line tells it apart from other comments and names the mission
 * in focus, so the next review can say a mission was completed; the last one holds the summary, so the dashboard and
 * the weekly e-mail read plain text instead of the Markdown.
 */
final class ReviewComment {

	static final String MARKER_PREFIX = "<!-- vaga-viva:review";

	private static final Pattern FOCUS = Pattern.compile("^<!-- vaga-viva:review focus=([a-z0-9-]+) -->");

	private static final Pattern SUMMARY = Pattern.compile("<!-- vaga-viva:summary ([A-Za-z0-9+/=]+) -->");

	private static final Pattern NUMBERING = Pattern.compile("^(?:\\d+[.)]|Pergunta \\d+:)\\s*");

	private static final JsonMapper JSON = JsonMapper.builder().build();

	/** Failing tests shown in the table; the rest is counted. */
	static final int MAX_FAILING_ROWS = 5;

	static final int MAX_QUESTIONS = 3;

	/** Squares in a progress bar; a mission with more tests is scaled down. */
	static final int MAX_SQUARES = 10;

	private static final String TITLE = "## 🔎 Revisão do seu PR";

	/** Unlike {@link ReviewTexts#FOOTER}, it does not repeat {@code /dica}: the next step already says it. */
	static final String FOOTER = "\n\n---\n<sub>Revisor do Vaga Viva: ele faz perguntas e nunca entrega a resposta.</sub>";

	private ReviewComment() {
	}

	/**
	 * What the review says in plain text: what already works and the questions. It is what the dashboard and the
	 * weekly e-mail show.
	 */
	record Summary(String working, List<String> questions) {

		Summary {
			questions = List.copyOf(questions);
		}

		/** Without the Markdown emphasis, for places that are not GitHub. */
		String plainText() {
			var text = new StringBuilder(working.replace("**", ""));
			for (int i = 0; i < questions.size(); i++) {
				text.append(i == 0 ? "\n\n" : "\n").append(i + 1).append(". ").append(questions.get(i).replace("**", ""));
			}
			return text.toString().strip();
		}

	}

	/** The usual review: one mission of the plan still has failing tests. */
	static String focus(ReviewContext context, Mission focus, List<FailedTest> failing, Summary summary,
			String testCommand, Optional<Mission> justCompleted) {
		List<Mission> plan = context.plan().missions();
		var body = new StringBuilder("<!-- vaga-viva:review focus=" + focus.id() + " -->\n" + TITLE + "\n\n");
		justCompleted.ifPresent(done -> {
			MissionTally tally = context.tallies().get(done.id());
			String tests = tally == null ? "os testes" : tally.total() == 1 ? "o teste" : "os " + tally.total() + " testes";
			body.append("🎉 **Missão ").append(order(plan, done)).append(" concluída:** ").append(tests)
				.append(" de **").append(done.title()).append("** passam. Agora é a vez da próxima.\n\n");
		});
		body.append("**Missão em foco:** ").append(order(plan, focus)).append(" · ").append(focus.title())
			.append(" · [contrato da missão](").append(contractUrl(context, focus)).append(")\n\n");
		body.append(progress(context, focus));
		body.append("### ✅ O que já está funcionando\n\n").append(summary.working()).append("\n\n");
		body.append(failingTable(failing));
		body.append("### 🤔 Perguntas para pensar\n\n");
		for (int i = 0; i < summary.questions().size(); i++) {
			body.append(i + 1).append(". ").append(summary.questions().get(i)).append('\n');
		}
		body.append("\n### 👉 Próximo passo\n\n")
			.append("1. Rode só os testes desta missão: `").append(testCommand).append("`\n")
			.append("2. Ajuste o código até eles passarem e faça um novo push. Eu reviso de novo.\n")
			.append("3. Travou? Comente `/dica` neste PR para receber uma dica, uma de cada vez.\n\n")
			.append("Todas as missões do seu plano estão nas [issues do repositório](https://github.com/")
			.append(context.repositoryFullName()).append("/issues).");
		return close(body, summary);
	}

	/** Every mission of the plan passes on the pull request. */
	static String allPassing(ReviewContext context) {
		var summary = new Summary("Todas as missões do seu plano passam neste PR.",
				List.of("Algum nome no seu código poderia explicar melhor o que ele faz?",
						"Algum trecho repetido caberia num lugar só?"));
		var body = new StringBuilder(MARKER_PREFIX + " -->\n" + TITLE + "\n\n");
		body.append("🎉 **Todas as missões do seu plano passam neste PR.** Parabéns!\n\n");
		body.append(progress(context, null));
		body.append("### 👉 Próximo passo\n\n")
			.append("1. Antes do merge, releia o seu código: ")
			.append(summary.questions().get(0).toLowerCase(Locale.ROOT)).append(' ')
			.append(summary.questions().get(1)).append('\n')
			.append("2. Faça o merge deste PR.");
		return close(body, summary);
	}

	/** The workflow finished without a test report: most often the code does not compile. */
	static String noReport() {
		var summary = new Summary("Os testes não rodaram neste PR: o workflow terminou sem relatório de testes.",
				List.of("Qual é a primeira mensagem de erro no passo Testes da aba Actions?"));
		var body = new StringBuilder(MARKER_PREFIX + " -->\n" + TITLE + "\n\n");
		body.append("### ⚠️ Os testes não rodaram\n\n")
			.append("O workflow **Vaga Viva** terminou sem gerar o relatório de testes. Na maioria das vezes, isso quer dizer que o projeto não compila.\n\n")
			.append("1. Abra a aba **Actions** deste PR.\n")
			.append("2. Entre no passo **Testes**.\n")
			.append("3. Procure a primeira mensagem de erro: em que arquivo e linha ela aponta?\n")
			.append("4. Corrija e faça um novo push. Eu reviso de novo.\n\n")
			.append("Se a primeira mensagem falar de download ou de rede, o problema não é o seu código: rode o workflow de novo em **Re-run jobs**.");
		return close(body, summary);
	}

	/**
	 * The text the student reads in the dashboard and the weekly e-mail: the summary of a review comment, or, for a
	 * comment written before the summary existed, the body without the marker and the footer.
	 */
	static Optional<String> plainText(String body) {
		if (body == null || !body.startsWith(MARKER_PREFIX)) {
			return Optional.empty();
		}
		return summary(body).map(Summary::plainText).or(() -> {
			String text = body.substring(body.indexOf("-->") + 3);
			for (String footer : List.of(ReviewTexts.FOOTER, FOOTER)) {
				if (text.endsWith(footer)) {
					text = text.substring(0, text.length() - footer.length());
				}
			}
			return Optional.of(text.strip());
		});
	}

	/** The summary a review comment carries; empty for older comments or a broken marker. */
	static Optional<Summary> summary(String body) {
		Matcher matcher = SUMMARY.matcher(body);
		if (!matcher.find()) {
			return Optional.empty();
		}
		try {
			byte[] json = Base64.getDecoder().decode(matcher.group(1));
			Map<?, ?> map = JSON.readValue(json, Map.class);
			Object working = map.get("working");
			Object questions = map.get("questions");
			if (!(working instanceof String text) || !(questions instanceof List<?> list)) {
				return Optional.empty();
			}
			return Optional.of(new Summary(text, list.stream().map(String::valueOf).toList()));
		}
		catch (RuntimeException e) {
			return Optional.empty();
		}
	}

	/** The mission the comment was about, from its first line; empty for other states and older comments. */
	static Optional<String> focusOf(String body) {
		Matcher matcher = FOCUS.matcher(body);
		return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
	}

	/** The LLM's questions, one each, without the numbering the comment adds itself, at most {@value #MAX_QUESTIONS}. */
	static List<String> cleanQuestions(List<String> questions) {
		return questions.stream()
			.map(q -> NUMBERING.matcher(q.strip()).replaceFirst("").strip())
			.filter(q -> !q.isEmpty())
			.limit(MAX_QUESTIONS)
			.toList();
	}

	/** The fixed "what already works", from the tallies, when the LLM gives nothing usable. */
	static String fallbackWorking(ReviewContext context, Mission focus) {
		MissionTally tally = context.tallies().get(focus.id());
		if (tally != null && tally.passed() > 0) {
			return "Na missão **" + focus.title() + "**, " + tally.passed() + " de " + tally.total()
					+ " testes já passam.";
		}
		return "Os testes da missão **" + focus.title() + "** ainda não passam: comece pelo primeiro da tabela abaixo.";
	}

	/** The fixed questions, from the first failing test, when the LLM gives nothing usable. */
	static List<String> fallbackQuestions(Mission focus, List<FailedTest> failing) {
		String test = "o teste **" + tableText(failing.getFirst().testName()) + "**";
		return List.of("O que " + test + " espera receber, e o que a sua implementação devolve hoje nesse caso?",
				"O que o contrato da missão, em missoes/" + focus.id() + ".md, diz sobre esse caso?");
	}

	private static String progress(ReviewContext context, Mission focus) {
		var table = new StringBuilder("### 📊 Seu progresso\n\n| Missão | Testes |\n| --- | --- |\n");
		List<Mission> plan = context.plan().missions();
		for (Mission mission : plan) {
			boolean inFocus = focus != null && mission.id().equals(focus.id());
			String name = order(plan, mission) + " · " + mission.title();
			table.append("| ").append(inFocus ? "**" + name + "**" : name).append(" | ")
				.append(bar(context.tallies().get(mission.id()), inFocus)).append(" |\n");
		}
		return table.append('\n').toString();
	}

	/** One square per test, green when it passes; red only on the mission in focus, so the rest reads as a path. */
	static String bar(MissionTally tally, boolean inFocus) {
		if (tally == null || tally.total() == 0) {
			return "sem resultado ainda";
		}
		int squares = Math.min(tally.total(), MAX_SQUARES);
		int green = tally.total() <= MAX_SQUARES ? tally.passed()
				: Math.round((float) tally.passed() * MAX_SQUARES / tally.total());
		String failing = inFocus ? "🟥" : "⬜";
		String done = tally.passed() == tally.total() ? " ✅" : "";
		return "🟩".repeat(green) + failing.repeat(squares - green) + " " + tally.passed() + "/" + tally.total() + done;
	}

	private static String failingTable(List<FailedTest> failing) {
		var table = new StringBuilder("### ❌ Testes que ainda falham\n\n| Teste | O que ele diz |\n| --- | --- |\n");
		failing.stream().limit(MAX_FAILING_ROWS).forEach(test -> table.append("| `")
			.append(tableText(test.testName()).replace("`", "'"))
			.append("` | ")
			.append(says(test))
			.append(" |\n"));
		if (failing.size() > MAX_FAILING_ROWS) {
			table.append("\n… e mais ").append(failing.size() - MAX_FAILING_ROWS)
				.append(failing.size() - MAX_FAILING_ROWS == 1 ? " teste.\n" : " testes.\n");
		}
		return table.append('\n').toString();
	}

	/** A test's message, quoted only when the filter would accept it from the LLM (the bug of #42). */
	private static String says(FailedTest test) {
		String message = test.message() == null ? "" : test.message().strip();
		if (message.isEmpty()) {
			return "falha sem mensagem de asserção";
		}
		if (!ReviewTextFilter.isQuotable(message)) {
			return "a mensagem não é repetida aqui: leia-a na aba **Actions**";
		}
		return tableText(message);
	}

	/** Neutralized for a comment and safe inside a Markdown table cell. */
	private static String tableText(String text) {
		return ReviewTextFilter.neutralize(text).replace("|", "\\|").replace("\n", " ").strip();
	}

	private static int order(List<Mission> plan, Mission mission) {
		return plan.indexOf(mission) + 1;
	}

	private static String contractUrl(ReviewContext context, Mission mission) {
		return "https://github.com/" + context.repositoryFullName() + "/blob/main/missoes/" + mission.id() + ".md";
	}

	private static String close(StringBuilder body, Summary summary) {
		var json = JSON.writeValueAsString(Map.of("working", summary.working(), "questions", summary.questions()));
		body.append("\n\n<!-- vaga-viva:summary ")
			.append(Base64.getEncoder().encodeToString(json.getBytes(UTF_8)))
			.append(" -->");
		return body.append(FOOTER).toString();
	}

}
