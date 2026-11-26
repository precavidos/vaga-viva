package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.PreviousComment;

/**
 * The hints' fixed texts, in Portuguese, and the hidden marker that tells the
 * next {@code /dica} which level comes next. Nothing is stored: the level is
 * read back from the app's own earlier comments on the pull request. The
 * fixed texts are the fallback whenever the LLM is off, fails or writes
 * something the filter refuses; they point and ask, and never show code.
 */
final class HintTexts {

	/** Levels 1 to 3 are hints; after the last one, the hints are over. */
	static final int LAST_LEVEL = 3;

	private static final Pattern MARKER = Pattern.compile("<!-- vaga-viva:hint mission=([\\w-]+) level=(\\d+) -->");

	private static final String FOOTER_START = "\n\n---\n<sub>";

	/** Separates the header from the hint's text, so earlier hints reach the LLM without their header. */
	private static final String TEXT_START = "<!-- vaga-viva:hint-text -->";

	/** What each level does: 1 says where to look, 2 the steps, 3 the exact point that fails. */
	static final List<String> LEVEL_NAMES = List.of("Direção", "Caminho", "Quase lá");

	private static final List<String> LEVEL_HINTS = List.of("(a próxima dica é mais específica)",
			"(a próxima dica aponta o ponto exato)", "(esta é a última dica)");

	private HintTexts() {
	}

	static String marker(String missionId, int level) {
		return "<!-- vaga-viva:hint mission=" + missionId + " level=" + level + " -->";
	}

	/** How many hints of levels 1 to {@value #LAST_LEVEL} about {@code missionId} the comments already gave. */
	static int hintsGiven(List<PreviousComment> comments, String missionId) {
		int count = 0;
		for (PreviousComment comment : comments) {
			Matcher marker = MARKER.matcher(comment.body());
			if (marker.lookingAt() && marker.group(1).equals(missionId)) {
				int level = Integer.parseInt(marker.group(2));
				if (level >= 1 && level <= LAST_LEVEL) {
					count++;
				}
			}
		}
		return count;
	}

	/** Whether {@code comment} is a hint of levels 1 to {@value #LAST_LEVEL} about {@code missionId}. */
	static boolean isHintAbout(PreviousComment comment, String missionId) {
		return hintsGiven(List.of(comment), missionId) == 1;
	}

	/**
	 * A leading "Nível 2:", "Dica 1 de 3 · Direção:" or "Quase lá:" the model writes although the header already
	 * says it, as on davsilvam/vaga-viva-clinic-scheduling#7.
	 */
	private static final Pattern LEVEL_PREFIX = Pattern.compile(
			"^\\s*(?:(?:n[íi]vel|dica)\\s*\\d+(?:\\s*de\\s*\\d+)?\\s*(?:[·,–-]\\s*)?(?:dire[çc][ãa]o|caminho|quase\\s+l[áa])?|dire[çc][ãa]o|caminho|quase\\s+l[áa])\\s*[:–-]\\s*",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	/** The LLM's hint without a level prefix the header already shows, starting with a capital letter. */
	static String withoutLevelPrefix(String text) {
		String stripped = LEVEL_PREFIX.matcher(text).replaceFirst("");
		if (stripped.length() == text.length() || stripped.isEmpty()) {
			return text;
		}
		return Character.toUpperCase(stripped.charAt(0)) + stripped.substring(1);
	}

	/** The text a hint comment carries, without its marker, header and footer. */
	static String text(PreviousComment comment) {
		String body = comment.body();
		int start = body.indexOf(TEXT_START);
		if (start >= 0) {
			body = body.substring(start + TEXT_START.length());
		}
		else {
			Matcher marker = MARKER.matcher(body);
			if (marker.lookingAt()) {
				body = body.substring(marker.end());
			}
		}
		int footer = body.indexOf(FOOTER_START);
		return (footer < 0 ? body : body.substring(0, footer)).strip();
	}

	/**
	 * A hint comment, in Portuguese, for students new to GitHub: the level and its name in the title, the mission, a
	 * {@code ●○○} marker and the text; the footer names the next level. The hidden marker on the first line is what
	 * the next {@code /dica} counts.
	 */
	static String comment(Mission mission, int level, String text) {
		if (level > LAST_LEVEL) {
			return marker(mission.id(), level) + "\n## 💡 As dicas desta missão acabaram\n\n**Missão:** " + mission.title()
					+ "\n\n" + TEXT_START + "\n\n" + text.strip() + FOOTER_START
					+ "Revisor do Vaga Viva: ele faz perguntas e nunca entrega a resposta.</sub>";
		}
		String dots = "●".repeat(level) + "○".repeat(LAST_LEVEL - level);
		return marker(mission.id(), level) + "\n## 💡 Dica " + level + " de " + LAST_LEVEL + " · "
				+ LEVEL_NAMES.get(level - 1) + "\n\n**Missão:** " + mission.title() + " · " + dots + " "
				+ LEVEL_HINTS.get(level - 1) + "\n\n" + TEXT_START + "\n\n" + text.strip() + FOOTER_START + footer(level)
				+ "</sub>";
	}

	private static String footer(int level) {
		if (level < LAST_LEVEL) {
			return "Ainda travado? Comente `/dica` de novo para a dica " + (level + 1) + " · "
					+ LEVEL_NAMES.get(level) + ".";
		}
		return "Esta foi a última dica desta missão. Releia o teste que falha e o contrato da missão.";
	}

	/** The fixed hint of {@code level} (1 to {@value #LAST_LEVEL}); {@code failing} may be empty. */
	static String fallback(int level, Mission mission, List<FailedTest> failing) {
		return switch (level) {
			case 1 -> "Uma direção para a missão **" + mission.title() + "**: " + mission.summary() + "\n\n"
					+ "Antes de mexer no código, pense no conceito por trás disso: que parte do sistema decide esse comportamento, e o que ela precisa saber para decidir?";
			case 2 -> "Uma abordagem para a missão **" + mission.title() + "**: releia o contrato em `missoes/"
					+ mission.id() + ".md` e anote, em palavras, cada coisa que ele pede: o que entra, o que sai e o que acontece nos casos de erro."
					+ (failing.isEmpty() ? ""
							: " Depois, compare essa lista com o teste **" + ReviewTextFilter.neutralize(failing.getFirst().testName())
									+ "**: qual item da lista ele verifica?")
					+ "\n\nQue passos, em ordem, levam do que entra ao que o contrato espera que saia?";
			case 3 -> almostThere(mission, failing);
			default -> throw new IllegalArgumentException("no hint of level " + level);
		};
	}

	private static String almostThere(Mission mission, List<FailedTest> failing) {
		String start = "Quase lá na missão **" + mission.title() + "**. ";
		if (failing.isEmpty()) {
			return start + "Rode só os testes desta missão e leia a primeira asserção que falha: o que ela esperava e o que recebeu? A diferença entre os dois é o ponto a corrigir.";
		}
		FailedTest first = failing.getFirst();
		String test = "O teste **" + ReviewTextFilter.neutralize(first.testName()) + "**";
		if (first.message().isBlank() || !ReviewTextFilter.isQuotable(first.message())) {
			String why = first.message().isBlank() ? " falha sem mensagem de asserção."
					: " falha com uma mensagem que eu não repito aqui.";
			return start + test + why + " Abra o teste e leia o que ele verifica por último: o que ele esperava receber, e o que a sua implementação devolve nesse caso?";
		}
		return start + test + " falha com esta mensagem:\n\n> " + ReviewTextFilter.neutralize(first.message().strip()) + "\n\n"
				+ "O que essa mensagem diz que era esperado, e o que a sua implementação devolve nesse caso? A diferença entre os dois é o ponto a corrigir.";
	}

	/** After the last level: no more hints, back to the test and the contract. */
	static String exhausted(Mission mission, List<FailedTest> failing) {
		String test = failing.isEmpty() ? "os testes desta missão" : "o teste **" + ReviewTextFilter.neutralize(failing.getFirst().testName()) + "**";
		return "As dicas da missão **" + mission.title() + "** acabaram neste PR: você já recebeu as " + LAST_LEVEL
				+ ".\n\nAgora é com você. Releia com calma " + test + " e o contrato em `missoes/" + mission.id()
				+ ".md`: o que o teste verifica, passo a passo, e em qual desses passos a sua implementação se afasta dele?";
	}

	/**
	 * The student named a mission that is not in their plan. What they wrote is
	 * not repeated, so a mention or a link in it never reaches the comment.
	 */
	static String unknownMission(List<Mission> plan) {
		return "Não encontrei essa missão no seu plano. Para pedir uma dica de uma missão específica, comente com o id dela:\n\n"
				+ plan.stream()
					.map(mission -> "- `/dica " + mission.id() + "` (" + mission.title() + ")")
					.collect(Collectors.joining("\n"))
				+ "\n\nOu comente só `/dica` para receber uma dica da primeira missão que ainda falha.";
	}

	/**
	 * No test report for the pull request's last commit. Unlike the reviewer,
	 * which only runs after the workflow completed, a {@code /dica} often
	 * arrives while the workflow is still running.
	 */
	static String noReport() {
		return """
				Ainda não encontrei o relatório de testes do último commit deste PR, então não sei qual missão está falhando.

				Se o workflow **Vaga Viva** ainda está rodando, espere ele terminar e comente `/dica` de novo. Se ele já terminou, abra a aba **Actions** deste PR e veja o passo **Testes**: qual é a primeira mensagem de erro? Você também pode pedir a dica de uma missão específica, como em `/dica` seguido do id dela.""";
	}

	static String missionPassing(Mission mission) {
		return "Os testes da missão **" + mission.title() + "** já passam neste PR, então não há dica a dar sobre ela. "
				+ "Comente só `/dica` para receber uma dica da primeira missão que ainda falha.";
	}

	static String allPassing() {
		return "Todos os testes das missões do seu plano passam neste PR, então não há dica a dar agora. "
				+ "Antes do merge, releia o seu código: algum nome poderia explicar melhor o que faz?";
	}

}
