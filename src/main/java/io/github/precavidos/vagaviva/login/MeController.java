package io.github.precavidos.vagaviva.login;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.precavidos.vagaviva.classroom.Teachers;

/** Who is signed in, for the top bar and the pages; 401 when nobody is. */
@RestController
class MeController {

	private final Teachers teachers;

	MeController(Teachers teachers) {
		this.teachers = teachers;
	}

	@GetMapping("/api/me")
	Me me(@AuthenticationPrincipal OAuth2User user) {
		Object attribute = user.getAttribute("login");
		String login = attribute == null ? user.getName() : attribute.toString();
		return new Me(login, teachers.teaches(login));
	}

	/** @param teaches the person owns a classroom, so the bar links to "Minhas turmas" */
	record Me(String login, boolean teaches) {
	}

}
