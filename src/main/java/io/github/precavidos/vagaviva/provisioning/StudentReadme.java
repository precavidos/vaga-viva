package io.github.precavidos.vagaviva.provisioning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.precavidos.vagaviva.plan.FictionalDomain;
import io.github.precavidos.vagaviva.plan.PlannedMission;

/**
 * Builds the "Seu projeto" section of a student's README: the intro text and
 * the table of missions, in plan order, linked to their issues. The section
 * text is student-facing (Portuguese); the code is not.
 */
final class StudentReadme {

	private static final String HEADING = "## Seu projeto";

	private StudentReadme() {
	}

	/**
	 * Returns the section: heading, intro, and a table of the missions in plan
	 * order, each linked to the issue found by its mission id. Ends with a
	 * newline.
	 */
	static String projectSection(String intro, List<PlannedMission> missions, List<MissionIssue> issues) {
		Map<String, MissionIssue> issueByMission = new HashMap<>();
		for (MissionIssue issue : issues) {
			issueByMission.putIfAbsent(issue.missionId(), issue);
		}
		StringBuilder section = new StringBuilder(HEADING + "\n\n" + intro + "\n\n");
		section.append("| Ordem | Missão | Issue |\n| --- | --- | --- |\n");
		for (int i = 0; i < missions.size(); i++) {
			PlannedMission planned = missions.get(i);
			MissionIssue issue = issueByMission.get(planned.mission().id());
			String link = issue == null ? "" : "[#%d](%s)".formatted(issue.number(), issue.htmlUrl());
			section.append("| %d | %s | %s |\n".formatted(i + 1, planned.mission().title(), link));
		}
		return section.toString();
	}

	/**
	 * Inserts the section (plus one blank line) before the first {@code ## }
	 * heading outside a fenced code block. Without such a heading, appends the
	 * section after one blank line. Every other line is kept as it was,
	 * including its line terminator.
	 */
	static String withProjectSection(String readme, String section) {
		List<String> lines = lines(readme);
		boolean inCodeBlock = false;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.startsWith("```")) {
				inCodeBlock = !inCodeBlock;
			}
			else if (!inCodeBlock && line.startsWith("## ")) {
				return String.join("", lines.subList(0, i)) + section + "\n"
						+ String.join("", lines.subList(i, lines.size()));
			}
		}
		return append(lines, section);
	}

	/** Returns a README for a new project: the title for the domain, then the section. */
	static String newReadme(FictionalDomain domain, String section) {
		return "# Vaga Viva: " + domain.name() + "\n\n" + section;
	}

	private static String append(List<String> lines, String section) {
		List<String> kept = new ArrayList<>(lines);
		while (!kept.isEmpty() && kept.get(kept.size() - 1).isBlank()) {
			kept.remove(kept.size() - 1);
		}
		StringBuilder out = new StringBuilder(String.join("", kept));
		if (!out.isEmpty()) {
			if (out.charAt(out.length() - 1) != '\n') {
				out.append('\n');
			}
			out.append('\n');
		}
		return out.append(section).toString();
	}

	/** Splits the text into lines, each keeping its own terminator. */
	private static List<String> lines(String text) {
		List<String> lines = new ArrayList<>();
		int start = 0;
		while (start < text.length()) {
			int end = text.indexOf('\n', start);
			end = end < 0 ? text.length() : end + 1;
			lines.add(text.substring(start, end));
			start = end;
		}
		return lines;
	}

}
