package io.github.precavidos.vagaviva.writing;

import java.util.ArrayList;
import java.util.List;

import io.github.precavidos.vagaviva.plan.PlannedMission;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/**
 * The fixed system prompt and the user message that describe a plan to the
 * model. The message carries only the plan's own data: never the posting's
 * text or the terms found in it (ADR 0003).
 */
final class MissionTextPrompt {

	static final String SYSTEM = """
			Você escreve textos curtos para alunos que vão construir um projeto de estudo no GitHub, montado a partir de uma vaga de emprego real. O plano do projeto já está decidido: você não muda, não reordena e não sugere missões.

			Escreva em português do Brasil, em segunda pessoa (você), num tom direto e encorajador, sem exageros.

			- projectIntro: um ou dois parágrafos curtos, sem títulos nem listas, dizendo para que vaga o projeto foi montado (nível e competências) e por que construir este projeto prepara o aluno para ela.
			- missions: para cada missão recebida, repita exatamente o mesmo id e escreva em context de duas a quatro frases ligando a missão às competências da vaga que ela exercita.

			Se a compatibilidade com a vaga for PARTIAL ou NOT_COVERED, o projectIntro diz numa frase quais tecnologias da vaga este projeto não pratica (as de "Não pratica") e que o que ele treina continua valendo para a vaga, sem prometer a stack que fica de fora. Se for COVERED ou NOT_STATED, não fale de compatibilidade.

			Nunca explique como resolver a missão, não escreva código, não use títulos Markdown e não invente requisitos, empresas, pessoas ou tecnologias que não estejam na entrada. Não use @ para mencionar ninguém nem # seguido de número.""";

	private static final String NONE = "(nenhuma)";

	private MissionTextPrompt() {
	}

	static String user(ProjectPlan plan, SkillVocabulary vocabulary) {
		var lines = new ArrayList<String>();
		lines.add("Arquétipo: " + plan.archetype());
		lines.add("Domínio fictício: " + plan.domain().name() + ". " + plan.domain().description());
		lines.add("Nível da vaga: " + plan.seniorityLevel().label());
		lines.add("Competências pedidas na vaga: " + joined(plan.postingSkills().stream()
			.map(id -> vocabulary.skillLabel(id))
			.toList()));
		lines.add("Tecnologias citadas na vaga: " + joined(plan.postingTechnologies().stream()
			.map(id -> vocabulary.technologyName(id))
			.toList()));
		var fit = plan.archetypeFit();
		lines.add("Compatibilidade com a vaga: " + fit.status() + ". O projeto pratica: " + joined(fit.covered())
				+ ". Não pratica: " + joined(fit.uncovered()) + ".");
		lines.add("");
		lines.add("Missões, na ordem do plano:");
		var missions = plan.missions();
		for (int i = 0; i < missions.size(); i++) {
			lines.addAll(missionLines(i + 1, missions.get(i), vocabulary));
		}
		return String.join("\n", lines);
	}

	private static List<String> missionLines(int order, PlannedMission planned, SkillVocabulary vocabulary) {
		var mission = planned.mission();
		return List.of(order + ". id: " + mission.id(), "   título: " + mission.title(),
				"   resumo: " + mission.summary(), "   competências da vaga que exercita: "
						+ joined(planned.matchedSkills().stream().map(id -> vocabulary.skillLabel(id)).toList()));
	}

	private static String joined(List<String> items) {
		return items.isEmpty() ? NONE : String.join("; ", items);
	}

}
