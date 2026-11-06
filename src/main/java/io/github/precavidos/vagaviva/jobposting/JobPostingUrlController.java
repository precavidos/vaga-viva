package io.github.precavidos.vagaviva.jobposting;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.precavidos.vagaviva.jobposting.JobPostingController.ExtractionResponse;

/**
 * Reads a job posting from the URL of a public career page and answers as if
 * its text had been pasted. The URL, the page and its text live only in memory
 * during the call: none of them is stored, logged or echoed (ADR 0003). Errors
 * are answered by {@link JobPostingUrlErrorHandler}.
 */
@RestController
@RequestMapping("/api/job-postings")
class JobPostingUrlController {

	/** Longest URL accepted; career page URLs are far shorter. */
	static final int MAX_URL_LENGTH = 2048;

	private final JobPostingFetcher fetcher;

	private final JobPostingPageReader reader;

	private final JobPostingExtractor extractor;

	private final PageReadLimiter limiter;

	JobPostingUrlController(JobPostingFetcher fetcher, JobPostingPageReader reader, JobPostingExtractor extractor,
			PageReadLimiter limiter) {
		this.fetcher = fetcher;
		this.reader = reader;
		this.extractor = extractor;
		this.limiter = limiter;
	}

	@PostMapping("/extraction/from-url")
	ExtractionResponse extractFromUrl(@Valid @RequestBody UrlExtractionRequest request) {
		return ExtractionResponse
			.from(limiter.call(() -> extractor.extract(reader.read(fetcher.fetch(request.url())))));
	}

	record UrlExtractionRequest(@NotBlank @Size(max = MAX_URL_LENGTH) String url) {
	}

}
