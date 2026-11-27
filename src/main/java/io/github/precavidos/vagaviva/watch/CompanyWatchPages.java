package io.github.precavidos.vagaviva.watch;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

/** "Empresas que você segue", a static page; hidden (404) while the company watch is off. */
@Controller
class CompanyWatchPages {

	private final CompanyWatch watch;

	CompanyWatchPages(CompanyWatch watch) {
		this.watch = watch;
	}

	@GetMapping("/empresas")
	String follows() {
		if (!watch.enabled()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return "forward:/empresas.html";
	}

}
