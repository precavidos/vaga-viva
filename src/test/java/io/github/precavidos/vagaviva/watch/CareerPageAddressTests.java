package io.github.precavidos.vagaviva.watch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.precavidos.vagaviva.watch.CareerPageAddress.Platform;

class CareerPageAddressTests {

	@ParameterizedTest
	@CsvSource({ "https://compass.gupy.io/, GUPY, https://compass.gupy.io/, Compass",
			"https://Compass.Gupy.io/?utm_source=x, GUPY, https://compass.gupy.io/, Compass",
			"https://job-boards.greenhouse.io/gitlab, GREENHOUSE, https://job-boards.greenhouse.io/gitlab, Gitlab",
			"https://boards.greenhouse.io/GitLab/, GREENHOUSE, https://boards.greenhouse.io/gitlab, Gitlab",
			"https://jobs.lever.co/palantir, LEVER, https://jobs.lever.co/palantir, Palantir",
			"https://jobs.ashbyhq.com/linear, ASHBY, https://jobs.ashbyhq.com/linear, Linear",
			"https://apply.workable.com/huggingface/, WORKABLE, https://apply.workable.com/huggingface, Huggingface",
			"https://careers.smartrecruiters.com/BoschGroup, SMARTRECRUITERS, https://careers.smartrecruiters.com/BoschGroup, BoschGroup",
			"https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite, WORKDAY, https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite, Nvidia",
			"https://nvidia.wd5.myworkdayjobs.com/en-US/NVIDIAExternalCareerSite, WORKDAY, https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite, Nvidia",
			"https://jobs.lever.co/acme-labs_br, LEVER, https://jobs.lever.co/acme-labs_br, Acme Labs Br" })
	void recognizesTheCareerPageOfEveryKnownPlatform(String url, Platform platform, String normalized, String name) {
		var page = CareerPageAddress.parse(url).orElseThrow();

		assertThat(page.platform()).isEqualTo(platform);
		assertThat(page.url()).isEqualTo(normalized);
		assertThat(page.suggestedName()).isEqualTo(name);
	}

	@ParameterizedTest
	@ValueSource(strings = { "https://www.catho.com.br/vagas/", "https://portal.gupy.io/", "https://www.gupy.io/",
			"https://job-boards.greenhouse.io/", "https://jobs.lever.co", "ftp://jobs.lever.co/palantir",
			"https://jobs.lever.co:8443/palantir", "https://nvidia.wd5.myworkdayjobs.com/", "not a url",
			"https://evilgupy.io/", "https://compass.gupy.io.evil.test/", "javascript:alert(1)" })
	void refusesAnythingElse(String url) {
		assertThat(CareerPageAddress.parse(url)).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({
			"https://compass.gupy.io/, /jobs/12811053?jobBoardSource=gupy_public_page, https://compass.gupy.io/jobs/12811053",
			"https://compass.gupy.io/, https://compass.gupy.io/jobs/12811053/, https://compass.gupy.io/jobs/12811053",
			"https://job-boards.greenhouse.io/gitlab, https://job-boards.greenhouse.io/gitlab/jobs/8556658002, https://job-boards.greenhouse.io/gitlab/jobs/8556658002",
			"https://boards.greenhouse.io/gitlab, https://job-boards.greenhouse.io/GitLab/jobs/8556658002?gh_src=x, https://job-boards.greenhouse.io/gitlab/jobs/8556658002",
			"https://jobs.lever.co/palantir, https://jobs.lever.co/palantir/00c2c97b-8514-4617-9883-e53e486b6dcd, https://jobs.lever.co/palantir/00c2c97b-8514-4617-9883-e53e486b6dcd",
			"https://jobs.ashbyhq.com/linear, https://jobs.ashbyhq.com/linear/6dd5500a-4bd4-4f57-ba09-7b07acefecf4, https://jobs.ashbyhq.com/linear/6dd5500a-4bd4-4f57-ba09-7b07acefecf4",
			"https://apply.workable.com/huggingface/, https://apply.workable.com/huggingface/j/1A2B3C4D5E/, https://apply.workable.com/huggingface/j/1A2B3C4D5E",
			"https://careers.smartrecruiters.com/BoschGroup, https://jobs.smartrecruiters.com/BoschGroup/744000012345678-software-engineer, https://jobs.smartrecruiters.com/BoschGroup/744000012345678-software-engineer",
			"https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite, /en-US/NVIDIAExternalCareerSite/job/US-CA-Santa-Clara/Software-Engineer_JR1234567, https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite/job/US-CA-Santa-Clara/Software-Engineer_JR1234567" })
	void turnsALinkOfThePageIntoItsPostingAddress(String page, String link, String posting) {
		assertThat(CareerPageAddress.parse(page).orElseThrow().posting(link)).contains(posting);
	}

	@ParameterizedTest
	@CsvSource({ "https://compass.gupy.io/, https://outra.gupy.io/jobs/12811053",
			"https://compass.gupy.io/, https://compass.gupy.io/candidates",
			"https://job-boards.greenhouse.io/gitlab, https://job-boards.greenhouse.io/outra/jobs/8556658002",
			"https://jobs.lever.co/palantir, https://jobs.lever.co/palantir/00c2c97b-8514-4617-9883-e53e486b6dcd/apply",
			"https://jobs.lever.co/palantir, https://jobs.lever.co/palantir?team=Dev",
			"https://jobs.ashbyhq.com/linear, https://jobs.ashbyhq.com/linear/about",
			"https://apply.workable.com/huggingface/, https://apply.workable.com/huggingface/j/1A2B3C4D5E/apply",
			"https://careers.smartrecruiters.com/BoschGroup, https://careers.smartrecruiters.com/BoschGroup/brazil",
			"https://nvidia.wd5.myworkdayjobs.com/NVIDIAExternalCareerSite, https://nvidia.wd5.myworkdayjobs.com/Outro/job/X/Y_1",
			"https://compass.gupy.io/, mailto:vagas@compass.test" })
	void ignoresLinksThatAreNotPostingsOfThatCompany(String page, String link) {
		assertThat(CareerPageAddress.parse(page).orElseThrow().posting(link)).isEmpty();
	}

	@Test
	void tellsAPostingOfAKnownPlatformFromAnyOtherAddress() {
		assertThat(CareerPageAddress.isKnownPosting("https://compass.gupy.io/jobs/12811053")).isTrue();
		assertThat(CareerPageAddress.isKnownPosting(
				"https://nvidia.wd5.myworkdayjobs.com/en-US/NVIDIAExternalCareerSite/job/US/Engineer_JR1")).isTrue();
		assertThat(CareerPageAddress.isKnownPosting("https://compass.gupy.io/")).isFalse();
		assertThat(CareerPageAddress.isKnownPosting("https://www.catho.com.br/vagas/dev/123")).isFalse();
		assertThat(CareerPageAddress.isKnownPosting("http://169.254.169.254/latest/meta-data")).isFalse();
	}

	@Test
	void refusesAddressesTooLongToStore() {
		assertThat(CareerPageAddress.parse("https://jobs.lever.co/" + "a".repeat(600))).isEmpty();
		assertThat(CareerPageAddress.parse("https://compass.gupy.io/").orElseThrow()
			.posting("https://compass.gupy.io/jobs/" + "1".repeat(600))).isEmpty();
	}

}
