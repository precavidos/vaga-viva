package io.github.precavidos.vagaviva.watch;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import io.github.precavidos.vagaviva.web.Pages;
import jakarta.servlet.http.HttpServletRequest;

/** "Empresas que você segue"; hidden (404) while the company watch is off. */
@Controller
class CompanyWatchPages {

	private final CompanyWatch watch;

	CompanyWatchPages(CompanyWatch watch) {
		this.watch = watch;
	}

	@GetMapping("/companies")
	ResponseEntity<Resource> follows() {
		if (!watch.enabled()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return Pages.page("companies");
	}

	/** The route the weekly e-mails already sent link to. */
	@GetMapping("/empresas")
	ResponseEntity<Void> oldFollows(HttpServletRequest request) {
		return Pages.movedTo("/companies", request);
	}

}
