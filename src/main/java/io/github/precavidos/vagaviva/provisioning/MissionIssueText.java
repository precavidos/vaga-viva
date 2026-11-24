package io.github.precavidos.vagaviva.provisioning;

import java.util.stream.Collectors;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/**
 * The student-facing (Portuguese) text of a mission issue. It says what to do first, points to the mission's
 * contract in the student's repository ({@code missoes/<id>.md}, written by the template), then why it matters for
 * the job and how to know it is done. {@code context} is the LLM's text or, when that is unavailable, the default
 * one (ADR 0002).
 */
final class MissionIssueText {

	private MissionIssueText() {
	}

	static String title(int order, Mission mission) {
		return "Missão " + order + ": " + mission.title();
	}

	/**
	 * @param repositoryFullName the student's repository, where {@code missoes/<id>.md} lives
	 * @param testCommand the archetype's command that runs only this mission's tests
	 */
	static String body(Mission mission, String context, SkillVocabulary vocabulary, String repositoryFullName,
			String testCommand) {
		String skills = mission.skills()
			.stream()
			.map(vocabulary::skillLabel)
			.collect(Collectors.joining(", "));
		String contract = "missoes/" + mission.id() + ".md";
		String contractUrl = "https://github.com/" + repositoryFullName + "/blob/main/" + contract;
		return """
				### O que fazer

				%s

				O contrato completo da missão (rotas, campos, respostas e erros esperados) está em [`%s`](%s). Os testes conferem exatamente o que ele descreve.

				### Por que esta missão importa para a vaga

				%s

				**Competências exercitadas:** %s

				### Como saber que terminou

				Rode só os testes desta missão:

				```bash
				%s
				```

				Eles falham até a missão estar pronta, e cada falha diz o que ainda falta. Quando todos passarem, abra um pull request: o revisor do Vaga Viva comenta com perguntas, sem entregar a resposta. Se travar, comente `/dica` no pull request para receber uma dica por vez.
				""".formatted(mission.summary(), contract, contractUrl, context, skills, testCommand);
	}

}
