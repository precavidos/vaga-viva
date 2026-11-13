package io.github.precavidos.vagaviva.email;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.Locale;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Records the signed-in student's consent to the weekly e-mail about their own
 * repository. The address is kept only for that purpose (ADR 0003).
 */
@RestController
@RequestMapping("/api/weekly-email")
class WeeklyEmailController {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final WeeklyEmailSubscriptionRepository subscriptions;

	private final Clock clock;

	WeeklyEmailController(WeeklyEmailSubscriptionRepository subscriptions, Clock clock) {
		this.subscriptions = subscriptions;
		this.clock = clock;
	}

	@PostMapping
	ResponseEntity<Void> consent(@Valid @RequestBody ConsentRequest request, @AuthenticationPrincipal OAuth2User user) {
		String login = login(user).toLowerCase(Locale.ROOT);
		String owner = request.repositoryFullName().substring(0, request.repositoryFullName().indexOf('/'));
		if (!owner.equalsIgnoreCase(login)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		WeeklyEmailSubscription subscription = subscriptions.findById(login)
			.orElseGet(() -> new WeeklyEmailSubscription(login));
		subscription.consent(request.email(), request.repositoryFullName(), newToken(), clock.instant());
		subscriptions.save(subscription);
		return ResponseEntity.noContent().build();
	}

	/** The token is the credential; the answer never reveals whether it matched. */
	@PostMapping(path = "/unsubscribe", consumes = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Void> unsubscribe(@RequestBody(required = false) UnsubscribeRequest request) {
		return unsubscribeWith(request == null ? null : request.token());
	}

	/** RFC 8058 one-click: {@code ?t=<token>} with the body {@code List-Unsubscribe=One-Click}. */
	@PostMapping(path = "/unsubscribe")
	ResponseEntity<Void> unsubscribeOneClick(@RequestParam(name = "t", required = false) String token) {
		return unsubscribeWith(token);
	}

	private ResponseEntity<Void> unsubscribeWith(String token) {
		if (token != null && !token.isBlank()) {
			subscriptions.findByUnsubscribeToken(token).ifPresent(subscriptions::delete);
		}
		return ResponseEntity.noContent().build();
	}

	private static String newToken() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String login(OAuth2User user) {
		Object login = user.getAttribute("login");
		return login == null ? user.getName() : login.toString();
	}

	record UnsubscribeRequest(String token) {
	}

	record ConsentRequest(@NotBlank @Email @Size(max = 254) String email,
			@NotBlank @Pattern(regexp = "[A-Za-z0-9-]+/[A-Za-z0-9._-]+") String repositoryFullName) {
	}

}
