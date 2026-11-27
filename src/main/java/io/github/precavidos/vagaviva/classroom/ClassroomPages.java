package io.github.precavidos.vagaviva.classroom;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriUtils;

import io.github.precavidos.vagaviva.web.Pages;
import jakarta.servlet.http.HttpServletRequest;

/** The teacher's classroom list, the dashboard and the invite, plus the Portuguese routes they had before. */
@Controller
class ClassroomPages {

	private static final String UUID = "[0-9a-f-]{36}";

	@GetMapping("/classrooms")
	ResponseEntity<Resource> classrooms() {
		return Pages.page("classrooms");
	}

	/** The dashboard; only a UUID is a classroom id, anything else is not a page. */
	@GetMapping("/classrooms/{id:" + UUID + "}")
	ResponseEntity<Resource> dashboard() {
		return Pages.page("classroom");
	}

	@GetMapping("/join/{joinCode}")
	ResponseEntity<Resource> invite() {
		return Pages.page("join");
	}

	@GetMapping("/professor")
	ResponseEntity<Void> oldClassrooms(HttpServletRequest request) {
		return Pages.movedTo("/classrooms", request);
	}

	@GetMapping("/turmas/{id:" + UUID + "}")
	ResponseEntity<Void> oldDashboard(@PathVariable String id, HttpServletRequest request) {
		return Pages.movedTo("/classrooms/" + id, request);
	}

	@GetMapping("/turma/{joinCode}")
	ResponseEntity<Void> oldInvite(@PathVariable String joinCode, HttpServletRequest request) {
		return Pages.movedTo("/join/" + UriUtils.encodePathSegment(joinCode, StandardCharsets.UTF_8), request);
	}

}
