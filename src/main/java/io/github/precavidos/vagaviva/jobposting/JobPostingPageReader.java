package io.github.precavidos.vagaviva.jobposting;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Turns a career page into the posting's text. It prefers the schema.org
 * {@code JobPosting} that many career sites embed as JSON-LD, which holds just
 * the posting; otherwise it takes the page's main text without navigation,
 * scripts and forms. Pages rendered by JavaScript have almost no text in their
 * HTML, so they are refused and the user is asked to paste the text instead.
 */
@Component
class JobPostingPageReader {

	/** Below this many characters the page most likely needs JavaScript to show the posting. */
	static final int MIN_TEXT_LENGTH = 200;

	private static final JsonMapper JSON = JsonMapper.builder().build();

	/** Never part of a posting. */
	private static final String NOT_CONTENT = "script, style, noscript, template, nav, aside";

	/** Page chrome when outside the content; inside an article a header holds the posting's title. */
	private static final String PAGE_CHROME = "header, footer";

	private static final String CONTENT = "main, article";

	/**
	 * Matches {@code application/ld+json} with or without parameters such as
	 * {@code ; charset=utf-8}.
	 */
	private static final String JSON_LD = "script[type~=(?i)^\\s*application/ld\\+json\\s*(;.*)?$]";

	/**
	 * The posting's text, at most {@link JobPostingExtractor#MAX_LENGTH} characters.
	 * @throws UnreadablePageException if the page has too little text
	 */
	String read(FetchedPage page) {
		Document document = parse(page);
		String jsonLd = jobPostingFromJsonLd(document);
		String text = jsonLd;
		if (text == null || text.length() < MIN_TEXT_LENGTH) {
			text = mainText(document);
			// A posting found in JSON-LD beats a page with no text of its own (a JavaScript shell).
			if (jsonLd != null && text.length() < MIN_TEXT_LENGTH) {
				return truncate(jsonLd);
			}
		}
		text = truncate(text);
		if (text.length() < MIN_TEXT_LENGTH) {
			throw new UnreadablePageException();
		}
		return text;
	}

	private static Document parse(FetchedPage page) {
		try {
			return Jsoup.parse(new ByteArrayInputStream(page.html()), page.charset(), "");
		}
		catch (IOException ex) {
			throw new UnreadablePageException();
		}
	}

	private static String jobPostingFromJsonLd(Document document) {
		for (Element script : document.select(JSON_LD)) {
			JsonNode posting;
			try {
				posting = findJobPosting(JSON.readTree(script.data()));
			}
			catch (JacksonException ex) {
				continue;
			}
			if (posting != null) {
				String title = htmlToText(posting.path("title").asString(""));
				String description = htmlToText(posting.path("description").asString(""));
				if (description.isEmpty()) {
					return null;
				}
				return title.isEmpty() ? description : title + "\n\n" + description;
			}
		}
		return null;
	}

	/** Looks in arrays, in {@code @graph} and at {@code @type} given as a string or an array. */
	private static JsonNode findJobPosting(JsonNode node) {
		if (node == null) {
			return null;
		}
		if (node.isArray()) {
			for (JsonNode child : node) {
				JsonNode found = findJobPosting(child);
				if (found != null) {
					return found;
				}
			}
			return null;
		}
		if (node.isObject()) {
			return isJobPosting(node.get("@type")) ? node : findJobPosting(node.get("@graph"));
		}
		return null;
	}

	private static boolean isJobPosting(JsonNode type) {
		if (type == null) {
			return false;
		}
		if (type.isArray()) {
			for (JsonNode each : type) {
				if (isJobPosting(each)) {
					return true;
				}
			}
			return false;
		}
		return "JobPosting".equals(type.asString(""));
	}

	/** The description is HTML, and some sites escape it twice ({@code &lt;p&gt;}). */
	private static String htmlToText(String html) {
		String text = Jsoup.parse(html).text();
		return text.contains("<") ? Jsoup.parse(text).text() : text;
	}

	private static String mainText(Document document) {
		document.select(NOT_CONTENT).remove();
		for (Element chrome : document.select(PAGE_CHROME)) {
			if (chrome.closest(CONTENT) == null) {
				chrome.remove();
			}
		}
		// Search and newsletter forms are short; some sites (ASP.NET) wrap the whole page in a form.
		for (Element form : document.select("form")) {
			if (form.text().length() < MIN_TEXT_LENGTH) {
				form.remove();
			}
		}
		for (String candidate : new String[] { "main", "article", "body" }) {
			Element element = document.selectFirst(candidate);
			if (element != null && !element.text().isBlank()) {
				return element.text();
			}
		}
		return "";
	}

	private static String truncate(String text) {
		if (text.length() <= JobPostingExtractor.MAX_LENGTH) {
			return text;
		}
		int end = JobPostingExtractor.MAX_LENGTH;
		if (Character.isHighSurrogate(text.charAt(end - 1))) {
			end--;
		}
		return text.substring(0, end);
	}

}
