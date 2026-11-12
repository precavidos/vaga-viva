package io.github.precavidos.vagaviva.email;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import io.github.precavidos.vagaviva.jobposting.JobPostingExtractor;

/**
 * Pulls the job posting out of an e-mail the student forwarded to us: drops the
 * student's own note, the forward line and its headers, and the signature.
 */
final class ForwardedPosting {

	private static final int HEADER_WINDOW = 6;

	private static final String BREAK = "\u0001";

	private static final Pattern FORWARD_LINE = Pattern.compile(
			"^(-{5,}\\s*(Forwarded message|Mensagem encaminhada)\\s*-{5,}|(Begin forwarded message|Início da mensagem encaminhada):)$",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern OUTLOOK_FROM = Pattern.compile("^(De|From):\\s.*", Pattern.CASE_INSENSITIVE);

	private static final Pattern OUTLOOK_SUBJECT = Pattern.compile("^(Assunto|Subject):\\s.*",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern HEADER_LINE = Pattern.compile(
			"^(De|From|Date|Data|Enviado|Sent|Para|To|Cc|Assunto|Subject):.*", Pattern.CASE_INSENSITIVE);

	private ForwardedPosting() {
	}

	/**
	 * The posting's text, never {@code null}, stripped, at most
	 * {@link JobPostingExtractor#MAX_LENGTH} characters.
	 */
	static String text(ReceivedEmail email) {
		String body = email.text().isBlank() ? htmlToText(email.html()) : email.text();
		List<String> lines = List.of(body.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1));

		int start = headerBlockStart(lines);
		if (start > 0) {
			while (start < lines.size()
					&& (lines.get(start).isBlank() || HEADER_LINE.matcher(lines.get(start)).matches())) {
				start++;
			}
		}
		int end = lines.size();
		for (int i = start; i < lines.size(); i++) {
			if (lines.get(i).equals("-- ")) {
				end = i;
				break;
			}
		}
		String result = String.join("\n", lines.subList(start, end)).strip();
		if (result.length() > JobPostingExtractor.MAX_LENGTH) {
			result = result.substring(0, JobPostingExtractor.MAX_LENGTH);
		}
		return result;
	}

	/** Index of the line where the forwarded headers begin, or 0 when nothing was forwarded. */
	private static int headerBlockStart(List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			if (FORWARD_LINE.matcher(lines.get(i).strip()).matches()) {
				return i + 1;
			}
		}
		for (int i = 0; i < lines.size(); i++) {
			if (!OUTLOOK_FROM.matcher(lines.get(i)).matches()) {
				continue;
			}
			for (int j = i + 1; j < lines.size() && j <= i + HEADER_WINDOW; j++) {
				if (OUTLOOK_SUBJECT.matcher(lines.get(j)).matches()) {
					return i;
				}
			}
		}
		return 0;
	}

	private static String htmlToText(String html) {
		if (html.isBlank()) {
			return "";
		}
		Document doc = Jsoup.parse(html);
		doc.select("br, p").after(BREAK);
		List<String> lines = new ArrayList<>();
		for (String line : doc.text().split(BREAK, -1)) {
			lines.add(line.strip());
		}
		return String.join("\n", lines);
	}

}
