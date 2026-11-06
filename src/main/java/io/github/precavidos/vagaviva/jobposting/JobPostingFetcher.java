package io.github.precavidos.vagaviva.jobposting;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.io.ModalCloseable;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

/**
 * Downloads a public career page so its posting can be read. URLs come from
 * users, so the fetcher guards against SSRF: only {@code http} and
 * {@code https}, only public addresses (checked by {@link PolicyDnsResolver} on
 * the addresses actually connected to), redirects followed by hand and checked
 * again, and limits on time and size. The page lives only in memory, and
 * nothing here logs the URL or the page (ADR 0003).
 */
@Component
class JobPostingFetcher implements AutoCloseable {

	static final Duration TIMEOUT = Duration.ofSeconds(8);

	static final int MAX_BYTES = 2 * 1024 * 1024;

	static final int MAX_REDIRECTS = 3;

	static final String USER_AGENT = "VagaViva/0.1";

	private static final Set<Integer> REDIRECT_STATUSES = Set.of(301, 302, 303, 307, 308);

	private static final Set<String> HTML_TYPES = Set.of("text/html", "application/xhtml+xml");

	private final CloseableHttpClient client;

	private final int maxBytes;

	JobPostingFetcher() {
		this(AddressPolicy.PUBLIC_ONLY, TIMEOUT, MAX_BYTES);
	}

	JobPostingFetcher(AddressPolicy policy, Duration timeout, int maxBytes) {
		this.maxBytes = maxBytes;
		Timeout limit = Timeout.of(timeout);
		var connections = PoolingHttpClientConnectionManagerBuilder.create()
			.setDnsResolver(new PolicyDnsResolver(policy))
			.setDefaultConnectionConfig(ConnectionConfig.custom().setConnectTimeout(limit).setSocketTimeout(limit).build())
			.build();
		this.client = HttpClients.custom()
			.setConnectionManager(connections)
			.setDefaultRequestConfig(RequestConfig.custom()
				.setConnectionRequestTimeout(limit)
				.setResponseTimeout(limit)
				.setRedirectsEnabled(false)
				.build())
			.disableRedirectHandling()
			.disableAutomaticRetries()
			.disableCookieManagement()
			.setUserAgent(USER_AGENT)
			.build();
	}

	/**
	 * Downloads the page at {@code url}.
	 * @throws UnsafeUrlException if the URL, or a redirect, is invalid, not
	 * http(s), or points to a non-public address
	 * @throws PageFetchException if the page cannot be downloaded
	 * @throws UnreadablePageException if the response is not HTML
	 */
	FetchedPage fetch(String url) {
		URI current = checked(parse(url));
		for (int redirects = 0;; redirects++) {
			Response response = get(current);
			if (response.page() != null) {
				return response.page();
			}
			if (redirects == MAX_REDIRECTS) {
				throw new PageFetchException();
			}
			current = checked(resolve(current, response.location()));
		}
	}

	private static URI parse(String url) {
		try {
			return new URI(url);
		}
		catch (URISyntaxException ex) {
			throw new UnsafeUrlException();
		}
	}

	private static URI resolve(URI current, String location) {
		try {
			return current.resolve(new URI(location));
		}
		catch (URISyntaxException | IllegalArgumentException ex) {
			throw new PageFetchException();
		}
	}

	private static URI checked(URI uri) {
		String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
		if (!(scheme.equals("http") || scheme.equals("https")) || uri.getHost() == null || uri.getHost().isEmpty()) {
			throw new UnsafeUrlException();
		}
		return uri;
	}

	private Response get(URI uri) {
		var request = new HttpGet(uri);
		request.setHeader("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.1");
		ClassicHttpResponse response;
		try {
			response = client.executeOpen(null, request, null);
		}
		catch (IOException ex) {
			throw isBlocked(ex) ? new UnsafeUrlException() : new PageFetchException();
		}
		try {
			return read(response);
		}
		catch (IOException ex) {
			throw new PageFetchException();
		}
		finally {
			// Drops the connection without draining the rest of a body that may be huge.
			if (response instanceof ModalCloseable closeable) {
				closeable.close(CloseMode.IMMEDIATE);
			}
		}
	}

	private Response read(ClassicHttpResponse response) throws IOException {
		int status = response.getCode();
		if (REDIRECT_STATUSES.contains(status)) {
			Header location = response.getFirstHeader("Location");
			if (location == null || location.getValue().isBlank()) {
				throw new PageFetchException();
			}
			return new Response(null, location.getValue());
		}
		if (status < 200 || status >= 300) {
			throw new PageFetchException();
		}
		HttpEntity entity = response.getEntity();
		if (entity == null) {
			return new Response(new FetchedPage(new byte[0], null), null);
		}
		ContentType type = contentType(entity);
		if (type != null && !HTML_TYPES.contains(type.getMimeType().toLowerCase(Locale.ROOT))) {
			throw new UnreadablePageException();
		}
		if (entity.getContentLength() > maxBytes) {
			throw new PageFetchException();
		}
		byte[] body;
		try (InputStream in = entity.getContent()) {
			body = in.readNBytes(maxBytes + 1);
		}
		if (body.length > maxBytes) {
			throw new PageFetchException();
		}
		return new Response(new FetchedPage(body, supportedCharset(type)), null);
	}

	private static ContentType contentType(HttpEntity entity) {
		String header = entity.getContentType();
		if (header == null || header.isBlank()) {
			return null;
		}
		try {
			return ContentType.parse(header);
		}
		catch (RuntimeException ex) {
			throw new UnreadablePageException();
		}
	}

	private static String supportedCharset(ContentType type) {
		String name = type == null ? null : type.getParameter("charset");
		if (name == null) {
			return null;
		}
		try {
			return Charset.isSupported(name) ? name : null;
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private static boolean isBlocked(Throwable ex) {
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t instanceof BlockedAddressException) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void close() {
		client.close(CloseMode.IMMEDIATE);
	}

	/** Either a downloaded page or the target of a redirect. */
	private record Response(FetchedPage page, String location) {
	}

}
