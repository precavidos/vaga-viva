package io.github.precavidos.vagaviva.review;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Whether a pull request's diff changes anything the reviewer could praise: a line of code or configuration.
 * Documentation, the plan, the template's tests, comments, blank lines and reindentation do not count. The LLM does
 * not decide this (ADR 0002): in an empty pull request it still praised a diagnosis the student never made.
 */
final class DiffChanges {

	private static final String FILE_START = "diff --git ";

	private static final Pattern NOT_CODE = Pattern.compile("(?:^(?:missoes|docs|\\.vaga-viva|src/test|tests?)/.*)"
			+ "|(?:.*\\.md)|(?:(?:.*/)?(?:LICENSE|\\.gitignore|\\.gitattributes|\\.dockerignore))");

	private static final Pattern SLASH_COMMENT = Pattern.compile("^(?://|/\\*|\\*(?:\\s|/|$)).*");

	private DiffChanges() {
	}

	static boolean changesCode(String diff) {
		if (diff == null) {
			return false;
		}
		String path = "";
		boolean inHunk = false;
		var removed = new ArrayList<String>();
		var added = new ArrayList<String>();
		for (String line : diff.lines().toList()) {
			if (line.startsWith(FILE_START)) {
				if (!removed.equals(added)) {
					return true;
				}
				removed.clear();
				added.clear();
				path = line.substring(line.lastIndexOf(" b/") + 3);
				inHunk = false;
			}
			else if (line.startsWith("@@")) {
				inHunk = true;
			}
			else if (inHunk && !NOT_CODE.matcher(path).matches()) {
				if (line.startsWith("+")) {
					keep(added, path, line);
				}
				else if (line.startsWith("-")) {
					keep(removed, path, line);
				}
			}
		}
		return !removed.equals(added);
	}

	private static void keep(List<String> lines, String path, String line) {
		String code = line.substring(1).strip();
		if (!code.isEmpty() && !isComment(path, code)) {
			lines.add(code);
		}
	}

	private static boolean isComment(String path, String code) {
		String name = path.substring(path.lastIndexOf('/') + 1);
		if (name.endsWith(".java") || name.endsWith(".ts") || name.endsWith(".js") || name.endsWith(".prisma")) {
			return SLASH_COMMENT.matcher(code).matches();
		}
		if (name.endsWith(".sql")) {
			return code.startsWith("--") || SLASH_COMMENT.matcher(code).matches();
		}
		if (name.endsWith(".xml")) {
			return code.startsWith("<!--") && code.endsWith("-->");
		}
		return code.startsWith("#");
	}

}
