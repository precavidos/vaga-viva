package io.github.precavidos.vagaviva.web;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The app's pages are static HTML kept out of {@code static/}, so their clean routes are their only address. A page
 * reads what it needs from {@code location} and calls the API, which is where login and permissions are decided.
 */
public final class Pages {

	private static final MediaType HTML = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

	private Pages() {
	}

	/** The page {@code pages/<name>.html} from the classpath. */
	public static ResponseEntity<Resource> page(String name) {
		return ResponseEntity.ok().contentType(HTML).body(new ClassPathResource("pages/" + name + ".html"));
	}

	/**
	 * A permanent redirect from an old route to {@code path}, keeping the query string: links in e-mails already sent
	 * and invites already shared point at the old routes.
	 */
	public static ResponseEntity<Void> movedTo(String path, HttpServletRequest request) {
		String query = request.getQueryString();
		return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
			.location(URI.create(query == null ? path : path + "?" + query))
			.build();
	}

}
