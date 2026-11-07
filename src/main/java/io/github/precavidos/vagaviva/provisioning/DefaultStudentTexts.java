package io.github.precavidos.vagaviva.provisioning;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import io.github.precavidos.vagaviva.plan.PlannedMission;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/**
 * Default, student-facing (Portuguese) project introduction and mission
 * contexts. They are used whenever the LLM-written texts are unavailable (ADR 0002).
 */
final class DefaultStudentTexts {

	private DefaultStudentTexts() {
	}

	static String projectIntro(ProjectPlan plan, SkillVocabulary vocabulary) {
		String level = plan.seniorityLevel().label();
		String opening = plan.postingSkills().isEmpty()
				? "Este projeto foi montado para uma vaga de nível " + level + "."
				: "Este projeto foi montado para uma vaga de nível " + level + " que pede "
						+ joinPt(plan.postingSkills().stream().map(id -> vocabulary.skillLabel(id)).toList()) + ".";
		String domain = plan.domain().description();
		return opening + " Ele se passa num domínio fictício, **" + plan.domain().name() + "**: "
				+ domain.substring(0, 1).toLowerCase(Locale.ROOT) + domain.substring(1)
				+ " Cada missão abaixo tem uma issue com o que fazer e testes que só passam quando ela estiver pronta.";
	}

	static String missionContext(PlannedMission mission, SkillVocabulary vocabulary) {
		if (mission.matchedSkills().isEmpty()) {
			return "Esta missão faz parte da base de todo projeto do Vaga Viva.";
		}
		return "Esta missão exercita "
				+ joinPt(mission.matchedSkills().stream().map(id -> vocabulary.skillLabel(id)).toList())
				+ ", pedidas na vaga.";
	}

	/** Joins items in Portuguese: "A", "A e B", "A, B e C". */
	static String joinPt(List<String> items) {
		if (items.size() <= 1) {
			return String.join("", items);
		}
		String head = items.subList(0, items.size() - 1).stream().collect(Collectors.joining(", "));
		return head + " e " + items.getLast();
	}

}
