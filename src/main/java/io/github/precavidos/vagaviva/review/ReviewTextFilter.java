package io.github.precavidos.vagaviva.review;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decides whether a text written by the LLM may be posted to a student: the
 * reviewer and the hints ask questions and never hand over code. The prompt
 * says so too, but a diff or a comment can try to talk the model out of it,
 * so the rule is also enforced here, conservatively: anything that looks like
 * code is refused and a fixed text is posted instead.
 */
public final class ReviewTextFilter {

	static final int MAX_LENGTH = 2_500;

	/** Inline code longer than this is more than an identifier or a route. */
	static final int MAX_INLINE_CODE = 40;

	private static final Pattern INLINE_CODE = Pattern.compile("`([^`\\n]*)`");

	/** Characters an identifier, a route or a file name does not need. */
	private static final Pattern CODE_CHARACTERS = Pattern.compile("[;{}()=<>\\[\\]]");

	/** Lines shaped like code: statements, blocks, declarations, arrows. */
	private static final Pattern CODE_LINE = Pattern.compile(
			"(?m)(;\\s*$|\\{\\s*$|^\\s*}|=>|->\\s*\\{|^\\s*(public|private|protected|class|interface|record|return|import|package|const|let|var|function|def|async|await|@\\w+\\s*$)\\b)");

	/** Four spaces or a tab at the start of a line: a Markdown code block. */
	private static final Pattern INDENTED_BLOCK = Pattern.compile("(?m)^( {4,}|\\t)\\S");

	/** A GitHub mention such as {@code @user}; an e-mail address is not one. */
	private static final Pattern MENTION = Pattern.compile("(?<![\\w.])@[A-Za-z0-9]");

	/** An issue reference such as {@code #2}; "C#" is not one. */
	private static final Pattern ISSUE_REFERENCE = Pattern.compile("(?<![\\w&])#\\d");

	private ReviewTextFilter() {
	}

	/**
	 * Makes text from the student's repository (test names, assertion
	 * messages) safe to place into one of the app's fixed comments: a zero-width
	 * space after {@code @} and {@code #} keeps GitHub from turning them into
	 * mentions or cross-references, and an HTML comment cannot fake the
	 * reviewer's or the hints' hidden markers.
	 */
	public static String neutralize(String text) {
		return text.replace("@", "@\u200B").replace("#", "#\u200B").replace("<!--", "&lt;!--");
	}

	public static boolean isSafe(String text) {
		if (text == null || text.isBlank() || text.length() > MAX_LENGTH) {
			return false;
		}
		if (text.contains("<!--")) {
			// Only the app writes hidden markers; one in a text could pass for the reviewer's or a hint's.
			return false;
		}
		if (text.contains("```") || text.contains("~~~") || INDENTED_BLOCK.matcher(text).find()) {
			return false;
		}
		if (text.lines().anyMatch(line -> line.stripLeading().startsWith("#"))) {
			return false;
		}
		Matcher inline = INLINE_CODE.matcher(text);
		while (inline.find()) {
			String code = inline.group(1);
			if (code.length() > MAX_INLINE_CODE || CODE_CHARACTERS.matcher(code).find()) {
				return false;
			}
		}
		String prose = INLINE_CODE.matcher(text).replaceAll("");
		return !CODE_LINE.matcher(prose).find() && !MENTION.matcher(text).find()
				&& !ISSUE_REFERENCE.matcher(text).find();
	}

}
