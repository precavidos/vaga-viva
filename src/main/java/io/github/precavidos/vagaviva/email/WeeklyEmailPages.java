package io.github.precavidos.vagaviva.email;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The unsubscribe page is a static file; this route gives it a clean address. It reads
 * the token from {@code location.search} and deletes nothing until the student clicks.
 */
@Controller
class WeeklyEmailPages {

	@GetMapping("/descadastrar")
	String unsubscribe() {
		return "forward:/descadastrar.html";
	}

}
