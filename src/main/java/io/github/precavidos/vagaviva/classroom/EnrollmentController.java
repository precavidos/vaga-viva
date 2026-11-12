package io.github.precavidos.vagaviva.classroom;

import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubApiException;
import io.github.precavidos.vagaviva.provisioning.InstallAppProblem;

/**
 * The student's side of a classroom: join through the invite link, which creates
 * their repository. The name is never logged. The messages stay in Portuguese.
 */
@RestController
@RequestMapping("/api/classrooms")
class EnrollmentController {

	static final int MAX_NAME_LENGTH = 40;

	private static final Logger log = LoggerFactory.getLogger(EnrollmentController.class);

	private final ClassroomService classrooms;

	private final EnrollmentService enrollments;

	private final GitHubApi github;

	private final String installUrl;

	EnrollmentController(ClassroomService classrooms, EnrollmentService enrollments, GitHubApi github,
			@Value("${vagaviva.github.app-slug:vaga-viva}") String appSlug) {
		this.classrooms = classrooms;
		this.enrollments = enrollments;
		this.github = github;
		this.installUrl = "https://github.com/apps/" + appSlug + "/installations/new";
	}

	@PostMapping("/{joinCode}/enrollments")
	ResponseEntity<?> join(@PathVariable String joinCode, @RequestBody JoinRequest request,
			@AuthenticationPrincipal OAuth2User user,
			@RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient client) {
		if (!request.consent()) {
			return badRequest("Para entrar na turma, aceite que o professor veja o seu progresso.");
		}
		String name = request.displayName() == null ? "" : request.displayName().strip();
		if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
			return badRequest("O nome deve ter de 1 a " + MAX_NAME_LENGTH + " caracteres.");
		}
		Classroom classroom = classrooms.byJoinCode(joinCode)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada."));
		String login = login(user);
		Optional<Enrollment> already = enrollments.existing(classroom, login);
		if (already.isPresent()) {
			return ResponseEntity.ok(Joined.from(already.get()));
		}
		String token = client.getAccessToken().getTokenValue();
		if (!github.isAppInstalledOn(token, login)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(InstallAppProblem.of(installUrl));
		}
		var joined = enrollments.join(classroom, login, name, token);
		Joined body = Joined.from(joined.enrollment());
		if (!joined.created()) {
			return ResponseEntity.ok(body);
		}
		return ResponseEntity.created(URI.create(body.htmlUrl())).body(body);
	}

	/** GitHub refused or failed: 401 when the token no longer works, so the page signs in again. */
	@ExceptionHandler(GitHubApiException.class)
	ProblemDetail gitHubFailed(GitHubApiException e) {
		log.warn("GitHub call failed while enrolling a student: status {}", e.status());
		if (e.status() == 401) {
			return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
					"Sua sessão do GitHub expirou. Entre de novo para criar o repositório.");
		}
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
				"O GitHub não conseguiu criar o repositório agora. Tente de novo em instantes.");
	}

	private static ResponseEntity<ProblemDetail> badRequest(String message) {
		return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message));
	}

	private static String login(OAuth2User user) {
		Object login = user.getAttribute("login");
		return login == null ? user.getName() : login.toString();
	}

	record JoinRequest(String displayName, boolean consent) {
	}

	record Joined(String fullName, String htmlUrl) {

		static Joined from(Enrollment enrollment) {
			return new Joined(enrollment.getRepositoryFullName(),
					"https://github.com/" + enrollment.getRepositoryFullName());
		}

	}

}
