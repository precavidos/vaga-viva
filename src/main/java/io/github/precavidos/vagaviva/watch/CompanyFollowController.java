package io.github.precavidos.vagaviva.watch;

import java.time.Clock;
import java.util.List;
import java.util.Locale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.precavidos.vagaviva.jobposting.ListingPageReader;

/**
 * "Empresas que você segue": the signed-in student who gets the weekly e-mail
 * follows up to {@value CompanyWatch#MAX_FOLLOWS} career pages. Following reads
 * the page at once and records its current postings as seen, so only postings
 * that appear later become alerts. The log names the login and the outcome,
 * never a URL (ADR 0003). Errors are answered by {@link CompanyFollowErrorHandler}.
 */
@RestController
@RequestMapping("/api/company-follows")
class CompanyFollowController {

	private static final Logger log = LoggerFactory.getLogger(CompanyFollowController.class);

	/** Most postings listed in the answer to a follow; the count covers them all. */
	static final int LISTED = 20;

	static final int MAX_NAME_LENGTH = 80;

	private final CompanyWatch watch;

	private final CompanyWatchStore store;

	private final ListingPageReader listings;

	private final Clock clock;

	CompanyFollowController(CompanyWatch watch, CompanyWatchStore store, ListingPageReader listings, Clock clock) {
		this.watch = watch;
		this.store = store;
		this.listings = listings;
		this.clock = clock;
	}

	@GetMapping
	ResponseEntity<FollowsResponse> list(@AuthenticationPrincipal OAuth2User user) {
		if (!watch.enabled()) {
			return ResponseEntity.notFound().build();
		}
		String login = login(user);
		List<FollowView> follows = store.follows(login).stream().map(FollowView::of).toList();
		return ResponseEntity.ok(new FollowsResponse(store.subscribed(login), CompanyWatch.MAX_FOLLOWS, follows));
	}

	@PostMapping
	ResponseEntity<?> follow(@Valid @RequestBody FollowRequest request, @AuthenticationPrincipal OAuth2User user) {
		if (!watch.enabled()) {
			return ResponseEntity.notFound().build();
		}
		String login = login(user);
		if (!store.subscribed(login)) {
			return problem(HttpStatus.FORBIDDEN,
					"Só quem recebe o e-mail semanal pode seguir empresas. Ative o e-mail semanal ao criar o seu projeto.");
		}
		var page = CareerPageAddress.parse(request.url());
		if (page.isEmpty()) {
			return problem(HttpStatus.BAD_REQUEST,
					"Use o link da lista de vagas da empresa na Gupy, Greenhouse, Lever, Ashby, Workable, "
							+ "SmartRecruiters ou Workday, por exemplo https://empresa.gupy.io/. Outros sites ainda não funcionam.");
		}
		var follows = store.follows(login);
		if (follows.stream().anyMatch(f -> f.url().equals(page.get().url()))) {
			return problem(HttpStatus.CONFLICT, "Você já segue esta empresa.");
		}
		if (follows.size() >= CompanyWatch.MAX_FOLLOWS) {
			return problem(HttpStatus.CONFLICT, "Você já segue " + CompanyWatch.MAX_FOLLOWS
					+ " empresas. Deixe de seguir uma para seguir outra.");
		}
		var listing = listings.read(page.get().url());
		if (listing.isEmpty()) {
			log.info("Company follow refused for {}: page unreadable", login);
			return problem(HttpStatus.BAD_GATEWAY,
					"Não conseguimos ler essa página agora. Confira o link e tente de novo daqui a pouco.");
		}
		var postings = ListedPostings.of(page.get(), listing.get());
		var name = request.name() == null || request.name().isBlank() ? page.get().suggestedName()
				: request.name().strip();
		long id = store.follow(login, page.get().url(), name, postings, clock.instant());
		log.info("Company followed by {} ({} postings open)", login, postings.size());
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(new FollowedResponse(id, name, page.get().url(), postings.size(),
					postings.stream().limit(LISTED).toList()));
	}

	@PatchMapping("/{id}")
	ResponseEntity<?> rename(@PathVariable long id, @Valid @RequestBody RenameRequest request,
			@AuthenticationPrincipal OAuth2User user) {
		if (!watch.enabled()) {
			return ResponseEntity.notFound().build();
		}
		return store.rename(login(user), id, request.name().strip()) ? ResponseEntity.noContent().build()
				: ResponseEntity.notFound().build();
	}

	@DeleteMapping("/{id}")
	ResponseEntity<Void> unfollow(@PathVariable long id, @AuthenticationPrincipal OAuth2User user) {
		if (!watch.enabled()) {
			return ResponseEntity.notFound().build();
		}
		return store.unfollow(login(user), id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}

	private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
		return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
	}

	private static String login(OAuth2User user) {
		Object login = user.getAttribute("login");
		return (login == null ? user.getName() : login.toString()).toLowerCase(Locale.ROOT);
	}

	record FollowRequest(@NotBlank @Size(max = 2048) String url, @Size(max = MAX_NAME_LENGTH) String name) {
	}

	record RenameRequest(@NotBlank @Size(max = MAX_NAME_LENGTH) String name) {
	}

	record FollowsResponse(boolean subscribed, int max, List<FollowView> follows) {
	}

	/** {@code checkLink} once the page failed {@value CompanyWatch#FAILURES_TO_WARN} weekly checks in a row. */
	record FollowView(long id, String name, String url, boolean checkLink) {

		static FollowView of(CompanyWatchStore.Follow follow) {
			return new FollowView(follow.pageId(), follow.name(), follow.url(),
					follow.consecutiveFailures() >= CompanyWatch.FAILURES_TO_WARN);
		}

	}

	record FollowedResponse(long id, String name, String url, int count, List<ListedPosting> postings) {
	}

}
