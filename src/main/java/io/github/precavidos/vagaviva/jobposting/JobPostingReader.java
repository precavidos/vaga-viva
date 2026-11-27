package io.github.precavidos.vagaviva.jobposting;

import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Reads a posting's text from its public URL: the app's own download first
 * and, when that page is a JavaScript shell or its site refuses the download,
 * the rendered reader. The URL and the text live only in memory (ADR 0003).
 */
@Component
public class JobPostingReader {

	private final JobPostingFetcher fetcher;

	private final JobPostingPageReader reader;

	private final RenderedPageReader rendered;

	private final PageReadLimiter limiter;

	JobPostingReader(JobPostingFetcher fetcher, JobPostingPageReader reader, RenderedPageReader rendered,
			PageReadLimiter limiter) {
		this.fetcher = fetcher;
		this.reader = reader;
		this.rendered = rendered;
		this.limiter = limiter;
	}

	/**
	 * The posting's text, or empty when it could not be read for any reason,
	 * including too many pages being read at once. Never throws.
	 */
	public Optional<String> read(String url) {
		try {
			return Optional.of(limiter.call(() -> text(url)));
		}
		catch (RuntimeException ex) {
			return Optional.empty();
		}
	}

	/**
	 * Without the limiter, which the caller holds. An unsafe URL never reaches
	 * the rendered reader: {@link UnsafeUrlException} is not caught here.
	 * @throws UnsafeUrlException, PageFetchException or UnreadablePageException
	 */
	String text(String url) {
		try {
			return reader.read(fetcher.fetch(url));
		}
		catch (UnreadablePageException | PageFetchException ex) {
			return rendered.read(url).orElseThrow(() -> ex);
		}
	}

}
