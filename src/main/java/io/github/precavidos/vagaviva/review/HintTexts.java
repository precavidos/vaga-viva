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
			if (marker.find() && marker.group(1).equals(missionId)) {
				int level = Integer.parseInt(marker.group(2));
				if (level >= 1 && level <= LAST_LEVEL) {
					count++;
				}
			}
		}
		return count;
	}

	/** A hint comment: the marker, the text and a footer that says where the student is. */
	static String comment(Mission mission, int level, String text) {
		return marker(mission.id(), level) + "\n" + text.strip() + FOOTER_START + footer(mission, level) + "</sub>";
	}

	private static String footer(Mission mission, int level) {
		if (level < LAST_LEVEL) {
			return "Dica " + level + " de " + LAST_LEVEL + " da missão **" + mission.title()
					+ "**. Comente `/dica` de novo para a próxima.";
		}
		if (level == LAST_LEVEL) {
			return "Dica " + level + " de " + LAST_LEVEL + " da missão **" + mission.title() + "**. Esta é a última.";
		}
		return "Revisor do Vaga Viva.";
	}

	/** The fixed hint of {@code level} (1 to {@value #LAST_LEVEL}); {@code failing} may be empty. */
	static String fallback(int level, Mission mission, List<FailedTest> failing) {
		return switch (level) {
			case 1 -> "Uma direção para a missão **" + mission.title() + "**: " + mission.summary() + "\n\n"
					+ "Antes de mexer no código, pense no conceito por trás disso: que parte do sistema decide esse comportamento, e o que ela precisa saber para decidir?";
			case 2 -> "Uma abordagem para a missão **" + mission.title() + "**: releia o contrato em `missoes/"
					+ mission.id() + ".md` e anote, em palavras, cada coisa que ele pede: o que entra, o que sai e o que acontece nos casos de erro."
					+ (failing.isEmpty() ? ""
							: " Depois, compare essa lista com o teste **" + failing.getFirst().testName()
									+ "**: qual item da lista ele verifica?")
					+ "\n\nQue passos, em ordem, levam do que entra ao que o contrato espera que saia?";
			case 3 -> failing.isEmpty()
					? "Quase lá na missão **" + mission.title() + "**. Rode só os testes desta missão e leia a primeira asserção que falha: o que ela esperava e o que recebeu? A diferença entre os dois é o ponto a corrigir."
					: "Quase lá na missão **" + mission.title() + "**. O teste **" + failing.getFirst().testName()
							+ "** falha com esta mensagem:\n\n> " + quote(failing.getFirst().message()) + "\n\n"
							+ "O que essa mensagem diz que era esperado, e o que a sua implementação devolve nesse caso? A diferença entre os dois é o ponto a corrigir.";
			default -> throw new IllegalArgumentException("no hint of level " + level);
		};
	}

	/** After the last level: no more hints, back to the test and the contract. */
	static String exhausted(Mission mission, List<FailedTest> failing) {
		String test = failing.isEmpty() ? "os testes desta missão" : "o teste **" + failing.getFirst().testName() + "**";
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

	static String missionPassing(Mission mission) {
		return "Os testes da missão **" + mission.title() + "** já passam neste PR, então não há dica a dar sobre ela. "
				+ "Comente só `/dica` para receber uma dica da primeira missão que ainda falha.";
	}

	static String allPassing() {
		return "Todos os testes das missões do seu plano passam neste PR, então não há dica a dar agora. "
				+ "Antes do merge, releia o seu código: algum nome poderia explicar melhor o que faz?";
	}

	private static String quote(String message) {
		return message.strip().replace("\n", "\n> ");
	}

}
