package io.github.precavidos.vagaviva.watch;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.precavidos.vagaviva.jobposting.ListingPage;
import io.github.precavidos.vagaviva.jobposting.ListingPage.PageLink;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** The postings of real (or real-shaped) listing pages, saved in Firecrawl's format. */
class ListedPostingsTests {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@ParameterizedTest
	@CsvSource(delimiter = ';', value = {
			"gupy; https://compass.gupy.io/; 6; https://compass.gupy.io/jobs/12873101; Agile Master Mid |Remoto",
			"greenhouse; https://job-boards.greenhouse.io/gitlab; 6; https://job-boards.greenhouse.io/gitlab/jobs/8877510002; Manager, Support Engineering (AMER)",
			"lever; https://jobs.lever.co/palantir; 4; https://jobs.lever.co/palantir/ac978161-6f46-4f6b-ad9e-a258e642751c; Administrative Business Partner",
			"ashby; https://jobs.ashbyhq.com/linear; 6; https://jobs.ashbyhq.com/linear/6dd5500a-4bd4-4f57-ba09-7b07acefecf4; Account Executive, Enterprise",
			"workable; https://apply.workable.com/huggingface/; 2; https://apply.workable.com/huggingface/j/3E1B2F4A9C; Senior Software Engineer - Hub",
			"smartrecruiters; https://careers.smartrecruiters.com/BoschGroup; 2; https://jobs.smartrecruiters.com/BoschGroup/744000098765432-desenvolvedor-a-java-junior; Desenvolvedor(a) Java Júnior",
			"workday; https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite; 2; https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite/job/US-CA-Santa-Clara/Software-Engineer--Deep-Learning-Frameworks_JR2001234; Software Engineer, Deep Learning Frameworks" })
	void findsEachPostingOnceInPageOrderWithItsTitle(String fixture, String pageUrl, int count, String firstUrl,
			String firstTitle) throws IOException {
		var page = CareerPageAddress.parse(pageUrl).orElseThrow();

		var postings = ListedPostings.of(page, listing(fixture));

		assertThat(postings).hasSize(count);
		assertThat(postings).extracting(ListedPosting::url).doesNotHaveDuplicates();
		assertThat(postings.get(0)).isEqualTo(new ListedPosting(firstUrl, firstTitle));
	}

	@Test
	void prefersARealTitleToAButtonText() {
		var page = CareerPageAddress.parse("https://jobs.lever.co/acme").orElseThrow();
		var id = "00c2c97b-8514-4617-9883-e53e486b6dcd";

		var postings = ListedPostings.of(page,
				new ListingPage(List.of(new PageLink("https://jobs.lever.co/acme/" + id, "Apply"),
						new PageLink("https://jobs.lever.co/acme/" + id, ""),
						new PageLink("https://jobs.lever.co/acme/" + id, "Pessoa Dev Java"))));

		assertThat(postings).containsExactly(new ListedPosting("https://jobs.lever.co/acme/" + id, "Pessoa Dev Java"));
	}

	@Test
	void keepsABlankTitleWhenThePageGivesNone() {
		var page = CareerPageAddress.parse("https://compass.gupy.io/").orElseThrow();

		var postings = ListedPostings.of(page,
				new ListingPage(List.of(new PageLink("https://compass.gupy.io/jobs/1", "Candidatar-se"))));

		assertThat(postings).containsExactly(new ListedPosting("https://compass.gupy.io/jobs/1", ""));
	}

	static ListingPage listing(String fixture) throws IOException {
		try (InputStream in = ListedPostingsTests.class.getResourceAsStream("/watch/listings/" + fixture + ".json")) {
			JsonNode data = JSON.readTree(in).path("data");
			var links = new ArrayList<PageLink>();
			// The same order FirecrawlListingReader gives: markdown links first, then the rest.
			var markdown = io.github.precavidos.vagaviva.jobposting.TestListings.markdownLinks(data.path("markdown").asString());
			links.addAll(markdown);
			for (JsonNode link : data.path("links")) {
				if (markdown.stream().noneMatch(l -> l.url().equals(link.asString()))) {
					links.add(new PageLink(link.asString(), ""));
				}
			}
			return new ListingPage(links);
		}
	}

}
