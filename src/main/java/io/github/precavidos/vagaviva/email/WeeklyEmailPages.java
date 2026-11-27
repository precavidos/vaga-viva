package io.github.precavidos.vagaviva.email;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import io.github.precavidos.vagaviva.web.Pages;
import jakarta.servlet.http.HttpServletRequest;

/**
 * The unsubscribe page reads the token from {@code location.search} and deletes nothing until the student clicks.
 */
@Controller
class WeeklyEmailPages {

	@GetMapping("/unsubscribe")
	ResponseEntity<Resource> unsubscribe() {
		return Pages.page("unsubscribe");
	}

	/** Every weekly e-mail already sent links here; the token in the query string must survive the redirect. */
	@GetMapping("/descadastrar")
	ResponseEntity<Void> oldUnsubscribe(HttpServletRequest request) {
		return Pages.movedTo("/unsubscribe", request);
	}

}
