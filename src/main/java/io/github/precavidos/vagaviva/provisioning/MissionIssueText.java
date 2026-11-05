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

	static String body(Mission mission, SkillVocabulary vocabulary) {
		String skills = mission.skills()
			.stream()
			.map(id -> vocabulary.skill(id).map(s -> s.label()).orElse(id))
			.collect(Collectors.joining(", "));
		return """
				%s

				**Competências exercitadas:** %s

				### Como saber que terminou

				Os testes desta missão passam:

				```bash
				./mvnw test -Dgroups=%s
				```

				Quando terminar, abra um pull request com a sua solução.
				""".formatted(mission.summary(), skills, mission.id());
	}

}
