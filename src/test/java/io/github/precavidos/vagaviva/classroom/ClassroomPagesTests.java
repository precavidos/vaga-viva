package io.github.precavidos.vagaviva.classroom;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

/**
 * The teacher and invite pages are static and open: the API behind them decides who may do what. MockMvc does
 * not follow a forward, so each route is checked for where it forwards and the page itself is fetched by name.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ClassroomPagesTests {

	@Autowired
	MockMvcTester mvc;

	@Test
	void theTeacherPageIsServedWithoutLogin() {
		assertThat(mvc.get().uri("/professor")).hasStatusOk().hasForwardedUrl("/professor.html");
		assertThat(mvc.get().uri("/professor.html")).hasStatusOk()
			.body()
			.asString()
			.contains("<title>Vaga Viva — Professor</title>");
	}

	@Test
	void theInvitePageIsServedWithoutLoginForAnyCode() {
		assertThat(mvc.get().uri("/turma/abcdefghij")).hasStatusOk().hasForwardedUrl("/turma.html");
		assertThat(mvc.get().uri("/turma.html")).hasStatusOk()
			.body()
			.asString()
			.contains("<title>Vaga Viva — Convite da turma</title>");
	}

	@Test
	void theSharedStylesAndScriptAreServed() {
		assertThat(mvc.get().uri("/vv.css")).hasStatusOk();
		assertThat(mvc.get().uri("/vv.js")).hasStatusOk();
	}

}
