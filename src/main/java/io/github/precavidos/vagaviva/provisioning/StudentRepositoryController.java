package io.github.precavidos.vagaviva.provisioning;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubApiException;
import io.github.precavidos.vagaviva.jobposting.JobPostingExtractor;
import io.github.precavidos.vagaviva.jobposting.ReceivesJobPostingText;
import io.github.precavidos.vagaviva.plan.ProjectPlanners;

/**
 * Creates the signed-in student's repository from a posting. The posting is
 * planned again here, from its text, so the plan cannot be tampered with in
 * the browser; the text lives only in memory during the call (ADR 0003). The
 * student-facing messages stay in Portuguese.
 */
@RestController
@ReceivesJobPostingText
@RequestMapping("/api")
class StudentRepositoryController {

	private static final Logger log = LoggerFactory.getLogger(StudentRepositoryController.class);

	private final JobPostingExtractor extractor;

	private final ProjectPlanners planner;

	private final StudentRepositoryProvisioner provisioner;

	private final GitHubApi github;

	private final String installUrl;

	StudentRepositoryController(JobPostingExtractor extractor, ProjectPlanners planner,
			StudentRepositoryProvisioner provisioner, GitHubApi github,
			@Value("${vagaviva.github.app-slug:vaga-viva}") String appSlug) {
		this.extractor = extractor;
		this.planner = planner;
		this.provisioner = provisioner;
		this.github = github;
		this.installUrl = "https://github.com/apps/" + appSlug + "/installations/new";
	}

	/** Who is signed in; 401 when nobody is. */
	@GetMapping("/me")
	Me me(@AuthenticationPrincipal OAuth2User user) {
		return new Me(login(user));
	}

	@PostMapping("/student-repositories")
	ResponseEntity<?> create(@Valid @RequestBody CreateRequest request, @AuthenticationPrincipal OAuth2User user,
			@RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient client) {
		String token = client.getAccessToken().getTokenValue();
		String login = login(user);
		if (!github.isAppInstalledOn(token, login)) {
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
					"Instale o app do Vaga Viva na sua conta do GitHub para criarmos o seu repositório.");
			problem.setProperty("installUrl", installUrl);
			return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
		}
		var plan = planner.plan(extractor.extract(request.text()));
		ProvisionedRepository repository = provisioner.provision(token, login, plan);
		return ResponseEntity.created(URI.create(repository.htmlUrl())).body(CreatedRepository.from(repository));
	}

	/** GitHub refused or failed: 401 when the token no longer works, so the page signs in again. */
	@ExceptionHandler(GitHubApiException.class)
	ProblemDetail gitHubFailed(GitHubApiException e) {
		log.warn("GitHub call failed while creating a student repository: status {}", e.status());
		if (e.status() == 401) {
			return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
					"Sua sessão do GitHub expirou. Entre de novo para criar o repositório.");
		}
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
				"O GitHub não conseguiu criar o repositório agora. Tente de novo em instantes.");
	}

	private static String login(OAuth2User user) {
		Object login = user.getAttribute("login");
		return login == null ? user.getName() : login.toString();
	}

	record CreateRequest(@NotBlank @Size(max = JobPostingExtractor.MAX_LENGTH) String text) {
	}

	record Me(String login) {
	}

	record CreatedRepository(String fullName, String htmlUrl, List<Issue> issues) {

		static CreatedRepository from(ProvisionedRepository repository) {
			return new CreatedRepository(repository.fullName(), repository.htmlUrl(), repository.missionIssues()
				.stream()
				.map((issue) -> new Issue(issue.missionId(), issue.number(), issue.htmlUrl()))
				.toList());
		}

	}

	record Issue(String missionId, int number, String htmlUrl) {
	}

}
