package io.github.precavidos.vagaviva.jobposting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class JobPostingUrlExtractionApiTests {

	private static final String URL = "https://vagas.exemplo.test/vaga-sigilosa-123";

	private static final String URL_MESSAGE = "Informe o endereço público de uma página de vaga, começando com http:// ou https://.";

	private static final String BODY_MESSAGE = "Corpo da requisição inválido: envie um JSON no formato {\"url\": \"...\"}.";

	private static final String FETCH_MESSAGE = "Não foi possível baixar a página da vaga. Confira o endereço ou cole o texto da vaga.";

	private static final String UNREADABLE_MESSAGE = "Não encontramos o texto da vaga nessa página; ela pode depender de JavaScript. Cole o texto da vaga.";

	private static final String UNEXPECTED_MESSAGE = "Erro inesperado ao ler a página da vaga. Tente de novo ou cole o texto da vaga.";

	private static final String BUSY_MESSAGE = "Muitas páginas sendo lidas agora. Tente de novo em instantes ou cole o texto da vaga.";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	MockMvcTester mvc;

	@Autowired
	JobPostingPageReader reader;

	@MockitoBean
	JobPostingFetcher fetcher;

	@MockitoSpyBean
	PageReadLimiter limiter;

	@Test
	void returnsTheSameExtractionAsPastingTheText() throws Exception {
		FetchedPage page = fixture("json-ld.html");
		given(fetcher.fetch(URL)).willReturn(page);

		MvcTestResult fromUrl = postUrl(URL);
		MvcTestResult pasted = mvc.post()
			.uri("/api/job-postings/extraction")
			.contentType(MediaType.APPLICATION_JSON)
			.content(JSON.writeValueAsString(Map.of("text", reader.read(page))))
			.exchange();

		assertThat(fromUrl).hasStatusOk();
		assertThat(pasted).hasStatusOk();
		assertThat(fromUrl).bodyJson()
			.isEqualTo(pasted.getResponse().getContentAsString(), JsonCompareMode.STRICT);
		assertThat(fromUrl).bodyJson().extractingPath("$.seniorityLevels").asArray().contains("JUNIOR");
		assertThat(fromUrl).bodyJson().extractingPath("$.technologies[*].id").asArray().contains("spring-boot");
	}

	@Test
	void refusesUnsafeUrls() {
		given(fetcher.fetch(anyString())).willThrow(new UnsafeUrlException());

		MvcTestResult response = postUrl("http://169.254.169.254/latest/meta-data/");

		assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo(URL_MESSAGE);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   ", "TOO_LONG" })
	void refusesBlankAndTooLongUrls(String url) {
		String value = url.equals("TOO_LONG") ? "https://x.test/" + "a".repeat(2048) : url;

		MvcTestResult response = postUrl(value);

		assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo(URL_MESSAGE);
		verifyNoInteractions(fetcher);
	}

	@Test
	void refusesMalformedJson() {
		MvcTestResult response = mvc.post()
			.uri("/api/job-postings/extraction/from-url")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{ \"url\": \"" + URL + "\" ")
			.exchange();

		assertThat(response).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo(BODY_MESSAGE);
	}

	@Test
	void reportsDownloadFailuresAsBadGateway() {
		given(fetcher.fetch(URL)).willThrow(new PageFetchException());

		MvcTestResult response = postUrl(URL);

		assertThat(response).hasStatus(HttpStatus.BAD_GATEWAY);
		assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo(FETCH_MESSAGE);
	}

	@Test
	void asksToPasteTheTextWhenThePageIsUnreadable() {
		given(fetcher.fetch(URL)).willReturn(fixture("spa-shell.html"));

		MvcTestResult response = postUrl(URL);

		assertThat(response).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
		assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo(UNREADABLE_MESSAGE);
	}

	@Test
	void reportsBusyAsServiceUnavailable() {
		willThrow(new TooManyPageReadsException()).given(limiter).call(any());

		MvcTestResult response = postUrl(URL);

		assertThat(response).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo(BUSY_MESSAGE);
	}

	@Test
	void unexpectedErrorsGetAFixedAnswerAndLogNoDetails(CapturedOutput log) {
		willThrow(new IllegalStateException("detalhe-sigiloso " + URL)).given(fetcher).fetch(URL);

		MvcTestResult response = postUrl(URL);

		assertThat(response).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response).bodyJson().extractingPath("$.detail").isEqualTo(UNEXPECTED_MESSAGE);
		assertThat(log.getAll()).doesNotContain("detalhe-sigiloso").doesNotContain("vaga-sigilosa-123");
	}

	@Test
	void neverEchoesOrLogsTheUrlOrThePage(CapturedOutput log) throws Exception {
		willReturn(fixture("json-ld.html")).given(fetcher).fetch(URL);
		String ok = postUrl(URL).getResponse().getContentAsString();

		willThrow(new UnsafeUrlException()).given(fetcher).fetch(URL);
		String unsafe = postUrl(URL).getResponse().getContentAsString();

		willThrow(new PageFetchException()).given(fetcher).fetch(URL);
		String failed = postUrl(URL).getResponse().getContentAsString();

		willReturn(fixture("spa-shell.html")).given(fetcher).fetch(URL);
		String unreadable = postUrl(URL).getResponse().getContentAsString();

		assertThat(ok + unsafe + failed + unreadable).doesNotContain("vaga-sigilosa-123");
		assertThat(log.getAll()).doesNotContain("vaga-sigilosa-123").doesNotContain("Spring Boot, PostgreSQL");
	}

	private MvcTestResult postUrl(String url) {
		return mvc.post()
			.uri("/api/job-postings/extraction/from-url")
			.contentType(MediaType.APPLICATION_JSON)
			.content(JSON.writeValueAsString(Map.of("url", url)))
			.exchange();
	}

	private static FetchedPage fixture(String name) {
		try (InputStream in = JobPostingUrlExtractionApiTests.class.getResourceAsStream("/jobposting/pages/" + name)) {
			return new FetchedPage(in.readAllBytes(), null);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

}
