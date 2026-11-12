package io.github.precavidos.vagaviva.classroom;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.github.precavidos.vagaviva.archetype.ArchetypeCatalog;
import io.github.precavidos.vagaviva.jobposting.JobPostingExtractor;
import io.github.precavidos.vagaviva.jobposting.ReceivesJobPostingText;
import io.github.precavidos.vagaviva.plan.PlannedMission;

/**
 * The teacher's side of a classroom: create one from a base posting, list one's
 * own, and the open invite page. The posting's text lives only in memory during
 * the call (ADR 0003). The messages stay in Portuguese.
 */
@RestController
@ReceivesJobPostingText
@RequestMapping("/api/classrooms")
class ClassroomController {

	private final ClassroomService service;

	private final ArchetypeCatalog archetypes;

	ClassroomController(ClassroomService service, ArchetypeCatalog archetypes) {
		this.service = service;
		this.archetypes = archetypes;
	}

	@PostMapping
	ResponseEntity<Created> create(@Valid @RequestBody CreateRequest request,
			@AuthenticationPrincipal OAuth2User user) {
		Classroom classroom = service.create(login(user), request.text(), request.name(),
				request.teacherEmail(), request.emailConsent());
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(new Created(classroom.getId().toString(), classroom.getJoinCode(),
					"/turma/" + classroom.getJoinCode(), classroom.getName()));
	}

	@GetMapping
	List<Summary> mine(@AuthenticationPrincipal OAuth2User user) {
		return service.ofTeacher(login(user))
			.stream()
			.map((c) -> new Summary(c.getId().toString(), c.getName(), c.getJoinCode(), service.studentCount(c)))
			.toList();
	}

	/** Open to anyone with the code; never lists the students. */
	@GetMapping("/{joinCode}")
	Invite invite(@PathVariable String joinCode) {
		Classroom classroom = service.byJoinCode(joinCode)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));
		StoredPlan stored = StoredPlan.fromJson(classroom.getPlan());
		var plan = stored.toProjectPlan(service.catalogOf(stored));
		var missions = new ArrayList<InviteMission>();
		for (PlannedMission planned : plan.missions()) {
			missions.add(new InviteMission(missions.size() + 1, planned.mission().id(), planned.mission().title()));
		}
		return new Invite(classroom.getName(), classroom.getTeacherLogin(),
				archetypes.archetype(plan.archetype()).name(), missions);
	}

	@ExceptionHandler(ClassroomService.NotATeacherException.class)
	ProblemDetail notATeacher() {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Só professores do piloto podem criar turmas.");
	}

	@ExceptionHandler(ClassroomService.InvalidClassroomException.class)
	ProblemDetail invalid(ClassroomService.InvalidClassroomException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
	}

	private static String login(OAuth2User user) {
		Object login = user.getAttribute("login");
		return login == null ? user.getName() : login.toString();
	}

	record CreateRequest(@NotBlank @Size(max = JobPostingExtractor.MAX_LENGTH) String text, String name,
			String teacherEmail, boolean emailConsent) {
	}

	record Created(String id, String joinCode, String inviteUrl, String name) {
	}

	record Summary(String id, String name, String joinCode, long students) {
	}

	record Invite(String name, String teacherLogin, String archetypeName, List<InviteMission> missions) {
	}

	record InviteMission(int order, String id, String title) {
	}

}
