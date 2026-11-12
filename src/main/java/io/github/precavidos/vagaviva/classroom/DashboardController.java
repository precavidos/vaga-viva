package io.github.precavidos.vagaviva.classroom;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The teacher's dashboard and student cards. Both routes sit below {@code /api/classrooms/{id}/}, never on
 * the single segment that the invite page leaves open, so they always require a sign-in. A classroom that
 * does not exist and one that belongs to another teacher both answer 404.
 */
@RestController
@RequestMapping("/api/classrooms/{id}")
class DashboardController {

	private static final String NOT_FOUND = "Turma não encontrada.";

	private final DashboardService service;

	DashboardController(DashboardService service) {
		this.service = service;
	}

	@GetMapping("/dashboard")
	DashboardService.Dashboard dashboard(@PathVariable String id, @AuthenticationPrincipal OAuth2User user) {
		return service.dashboard(classroomId(id), login(user)).orElseThrow(DashboardController::notFound);
	}

	@GetMapping("/students/{login}")
	DashboardService.StudentCard student(@PathVariable String id, @PathVariable("login") String studentLogin,
			@AuthenticationPrincipal OAuth2User user) {
		return service.student(classroomId(id), studentLogin, login(user)).orElseThrow(DashboardController::notFound);
	}

	private static UUID classroomId(String id) {
		try {
			return UUID.fromString(id);
		}
		catch (IllegalArgumentException e) {
			throw notFound();
		}
	}

	private static ResponseStatusException notFound() {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND);
	}

	private static String login(OAuth2User user) {
		Object login = user.getAttribute("login");
		return login == null ? user.getName() : login.toString();
	}

}
