package io.github.precavidos.vagaviva.login;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.precavidos.vagaviva.classroom.Teachers;
import io.github.precavidos.vagaviva.watch.CompanyWatch;

/** Who is signed in, for the top bar and the pages; 401 when nobody is. */
@RestController
class MeController {

	private final Teachers teachers;

	private final CompanyWatch companyWatch;

	MeController(Teachers teachers, CompanyWatch companyWatch) {
		this.teachers = teachers;
		this.companyWatch = companyWatch;
	}

	@GetMapping("/api/me")
	Me me(@AuthenticationPrincipal OAuth2User user) {
		Object attribute = user.getAttribute("login");
		String login = attribute == null ? user.getName() : attribute.toString();
		return new Me(login, teachers.teaches(login), companyWatch.enabled());
	}

	/**
	 * @param teaches the person owns a classroom, so the bar links to "Minhas turmas"
	 * @param companyWatch the company watch is on, so the bar links to "Empresas" (off, that page answers 404)
	 */
	record Me(String login, boolean teaches, boolean companyWatch) {
	}

}
