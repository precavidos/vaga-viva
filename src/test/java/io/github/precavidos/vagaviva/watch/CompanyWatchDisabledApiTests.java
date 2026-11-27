package io.github.precavidos.vagaviva.watch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;
import io.github.precavidos.vagaviva.jobposting.JobPostingReader;

/**
 * A blank {@code COMPANY_WATCH_SECRET} changes nothing: every route of the
 * company watch answers 404, nothing is read, and the weekly e-mail gets no
 * section. Firecrawl is on here, so only the secret turns it off.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "vagaviva.company-watch.secret=", "vagaviva.firecrawl.api-key=fc-test" })
@AutoConfigureMockMvc
class CompanyWatchDisabledApiTests {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	ClientRegistrationRepository registrations;

	@Autowired
	CompanyNews news;

	@MockitoBean
	JobPostingReader postings;

	@Test
	void everyRouteIsOff() {
		var maria = oauth2Login().clientRegistration(registrations.findByRegistrationId("github"))
			.attributes(attributes -> attributes.put("login", "maria"));

		assertThat(mvc.get().uri("/empresas").exchange()).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.get().uri("/api/company-follows").with(maria).exchange()).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.post()
			.uri("/api/company-follows")
			.with(maria)
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"url\":\"https://compass.gupy.io/\"}")
			.exchange()).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.delete().uri("/api/company-follows/1").with(maria).with(csrf()).exchange())
			.hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.post().uri(CompanyWatchCheckController.PATH).header("X-Vaga-Viva-Secret", "").exchange())
			.hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.post()
			.uri(PostingTextController.PATH)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"url\":\"https://compass.gupy.io/jobs/1\"}")
			.exchange()).hasStatus(HttpStatus.NOT_FOUND);
		verifyNoInteractions(postings);
	}

	@Test
	void theWeeklyEmailGetsNoSection() {
		assertThat(news.forStudent("maria", "java-spring")).isEmpty();
	}

}
