package io.github.precavidos.vagaviva.provisioning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.precavidos.vagaviva.plan.FictionalDomain;
import io.github.precavidos.vagaviva.plan.PlannedMission;

/**
 * Builds the "Seu projeto" section of a student's README: the intro text and
 * the table of missions, in plan order, linked to their issues. The section
 * text is student-facing (Portuguese); the code is not.
 */
final class StudentReadme {

	private static final String HEADING = "## Seu projeto";

	private static final Pattern OPENING_FENCE = Pattern.compile(" {0,3}(`{3,}|~{3,})(.*)");

	private static final Pattern CLOSING_FENCE = Pattern.compile(" {0,3}(`{3,}|~{3,})[ \\t]*");

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
	 * heading outside a fenced code block (backtick or tilde fences, as in CommonMark). Without such a heading, appends the
	 * section after one blank line. Every other line is kept as it was,
	 * including its line terminator.
	 */
	static String withProjectSection(String readme, String section) {
		List<String> lines = lines(readme);
		String fence = null;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String content = stripTerminator(line);
			if (fence == null) {
				fence = openingFence(content);
				if (fence == null && line.startsWith("## ")) {
					return String.join("", lines.subList(0, i)) + section + "\n"
							+ String.join("", lines.subList(i, lines.size()));
				}
			}
			else if (closesFence(content, fence)) {
				fence = null;
			}
		}
		return append(lines, section);
	}

	/**
	 * The fence that a line opens (3 or more backticks or tildes after at most
	 * three spaces), or null if it opens none. A backtick fence's info string may
	 * not contain backticks, as in CommonMark.
	 */
	private static String openingFence(String line) {
		Matcher matcher = OPENING_FENCE.matcher(line);
		if (!matcher.matches()) {
			return null;
		}
		String fence = matcher.group(1);
		return fence.charAt(0) == '`' && matcher.group(2).contains("`") ? null : fence;
	}

	/** A closing fence repeats the opening character at least as many times, with nothing else on the line. */
	private static boolean closesFence(String line, String fence) {
		Matcher matcher = CLOSING_FENCE.matcher(line);
		if (!matcher.matches()) {
			return false;
		}
		String candidate = matcher.group(1);
		return candidate.charAt(0) == fence.charAt(0) && candidate.length() >= fence.length();
	}

	private static String stripTerminator(String line) {
		int end = line.length();
		while (end > 0 && (line.charAt(end - 1) == '\n' || line.charAt(end - 1) == '\r')) {
			end--;
		}
		return line.substring(0, end);
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
