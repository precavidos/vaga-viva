package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

/**
 * The teacher and invite pages are static and open: the API behind them decides who may do what. Their clean routes
 * are their only address, and the Portuguese routes they had before redirect for good.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ClassroomPagesTests {

	private static final String ID = "3f2b8c1e-5a4d-4e6f-9b0a-1c2d3e4f5a6b";

	@Autowired
	MockMvcTester mvc;

	@Test
	void theClassroomListIsServedWithoutLogin() {
		assertThat(mvc.get().uri("/classrooms")).hasStatusOk()
			.hasContentTypeCompatibleWith(MediaType.TEXT_HTML)
			.bodyText()
			.contains("<title>Vaga Viva — Professor</title>");
	}

	@Test
	void theInvitePageIsServedWithoutLoginForAnyCode() {
		assertThat(mvc.get().uri("/join/abcdefghij")).hasStatusOk()
			.bodyText()
			.contains("<title>Vaga Viva — Convite da turma</title>");
	}

	@Test
	void theDashboardPageIsServedWithoutLoginForAnyClassroomId() {
		assertThat(mvc.get().uri("/classrooms/" + ID)).hasStatusOk()
			.bodyText()
			.contains("<title>Vaga Viva — Painel da turma</title>");
	}

	@Test
	void theDashboardRouteOnlyTakesAUuid() {
		assertThat(mvc.get().uri("/classrooms/abc")).hasStatus(404);
		assertThat(mvc.get().uri("/classrooms/" + "x".repeat(36))).hasStatus(404);
	}

	@Test
	void thePageFilesAreNotAddressesOfTheirOwn() {
		for (String file : new String[] { "/classrooms.html", "/classroom.html", "/join.html", "/companies.html",
				"/unsubscribe.html", "/pages/join.html", "/professor.html", "/turma.html", "/painel.html" }) {
			assertThat(mvc.get().uri(file)).hasStatus(404);
		}
	}

	@Test
	void theOldRoutesRedirectForGood() {
		assertThat(mvc.get().uri("/professor")).hasStatus(HttpStatus.MOVED_PERMANENTLY).hasRedirectedUrl("/classrooms");
		assertThat(mvc.get().uri("/turmas/" + ID)).hasStatus(HttpStatus.MOVED_PERMANENTLY)
			.hasRedirectedUrl("/classrooms/" + ID);
		assertThat(mvc.get().uri("/turma/abcdefghij")).hasStatus(HttpStatus.MOVED_PERMANENTLY)
			.hasRedirectedUrl("/join/abcdefghij");
		assertThat(mvc.get().uri("/turmas/abc")).hasStatus(404);
	}

	@Test
	void theSharedStylesAndScriptAreServed() {
		assertThat(mvc.get().uri("/vv.css")).hasStatusOk();
		assertThat(mvc.get().uri("/vv.js")).hasStatusOk();
	}

	/** "/" is the welcome page, so it is fetched by the name it forwards to. */
	@Test
	void pagesLoadTheMotionLayer() {
		assertThat(mvc.get().uri("/")).hasForwardedUrl("index.html");
		for (String page : new String[] { "/index.html", "/classrooms", "/join/abcdefghij", "/classrooms/" + ID }) {
			assertThat(mvc.get().uri(page)).hasStatusOk().bodyText().contains("vv-motion.js");
		}
		assertThat(mvc.get().uri("/vv-motion.js")).hasStatusOk();
		assertThat(mvc.get().uri("/vv-motion.css")).hasStatusOk();
	}

}
