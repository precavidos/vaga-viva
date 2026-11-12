package io.github.precavidos.vagaviva.classroom;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The classroom pages are static files; these routes give them clean addresses. The
 * pages read the invite code from {@code location.pathname} and call the API, which
 * is where login and permissions are decided.
 */
@Controller
class ClassroomPages {

	@GetMapping("/professor")
	String teacher() {
		return "forward:/professor.html";
	}

	@GetMapping("/turma/{joinCode}")
	String invite() {
		return "forward:/turma.html";
	}

}
