package io.github.precavidos.vagaviva.jobposting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.precavidos.vagaviva.jobposting.ListingPage.PageLink;

/**
 * The links of a page rendered as markdown, with their text. A job card is
 * often one link wrapping a logo and several lines (title, place, contract):
 * only its first line of text that is not a label is kept, which is the
 * posting's title. A link that appears twice is kept twice, since only one of
 * them may carry the title (Lever's "Apply" button comes first).
 */
final class MarkdownLinks {

	static final int MAX_TEXT_LENGTH = 150;

	/** The end of a link: {@code ](target)} or {@code ](target "title")}. */
	private static final Pattern TARGET = Pattern.compile("\\]\\(\\s*<?([^\\s)>]+)>?(?:\\s+\"[^\"]*\")?\\s*\\)");

	private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*\\]\\([^)]*\\)");

	private static final Pattern EMPHASIS = Pattern.compile("[*_`#>]+");

	private MarkdownLinks() {
	}

	static List<PageLink> parse(String markdown) {
		var links = new ArrayList<PageLink>();
		Matcher target = TARGET.matcher(markdown);
		while (target.find()) {
			int open = opening(markdown, target.start());
			if (open < 0 || (open > 0 && markdown.charAt(open - 1) == '!')) {
				continue;
			}
			links.add(new PageLink(target.group(1), firstLine(markdown.substring(open + 1, target.start()))));
		}
		return links;
	}

	/** The {@code [} that matches the {@code ]} at {@code close}, or -1. */
	private static int opening(String markdown, int close) {
		int depth = 0;
		for (int i = close - 1; i >= 0; i--) {
			char c = markdown.charAt(i);
			if (c == ']') {
				depth++;
			}
			else if (c == '[') {
				if (depth == 0) {
					return i;
				}
				depth--;
			}
		}
		return -1;
	}

	private static String firstLine(String text) {
		String plain = IMAGE.matcher(text).replaceAll(" ");
		for (String line : plain.split("\\R")) {
			String clean = EMPHASIS.matcher(line).replaceAll("").replaceAll("\\s+", " ").strip();
			// A label such as "Vaga para Efetivo:" comes before the title on Gupy's cards.
			if (!clean.isEmpty() && !clean.endsWith(":")) {
				return clean.length() > MAX_TEXT_LENGTH ? clean.substring(0, MAX_TEXT_LENGTH) : clean;
			}
		}
		return "";
	}

}
