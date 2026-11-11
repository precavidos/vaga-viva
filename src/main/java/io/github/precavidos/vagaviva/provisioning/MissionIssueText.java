package io.github.precavidos.vagaviva.provisioning;

import java.util.stream.Collectors;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/**
 * Default, student-facing (Portuguese) text for mission issues. It is what
 * students get whenever the LLM-written text is unavailable (ADR 0002).
 */
final class MissionIssueText {

	private MissionIssueText() {
	}

	static String title(int order, Mission mission) {
		return "Missão " + order + ": " + mission.title();
	}

	/** @param testCommand the archetype's command that runs only this mission's tests */
	static String body(Mission mission, String context, SkillVocabulary vocabulary, String testCommand) {
		String skills = mission.skills()
			.stream()
			.map(vocabulary::skillLabel)
			.collect(Collectors.joining(", "));
		return """
				### Por que esta missão importa para a vaga

				%s

				%s

				**Competências exercitadas:** %s

				### Como saber que terminou

				Os testes desta missão passam:

				```bash
				%s
				```

				Quando terminar, abra um pull request com a sua solução.
				""".formatted(context, mission.summary(), skills, testCommand);
	}

}
