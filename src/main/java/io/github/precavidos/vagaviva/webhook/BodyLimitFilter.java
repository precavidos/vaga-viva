package io.github.precavidos.vagaviva.webhook;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Keeps an anonymous webhook from loading an arbitrarily large body into the heap
 * before its signature is checked. A {@code Content-Length} above the limit is
 * answered with a 413 without reading anything; otherwise at most limit + 1 bytes
 * are read, so a chunked body or a wrong {@code Content-Length} cannot get past it
 * either. The controller then sees the same bytes it would have read itself.
 * Nothing about the request is logged (ADR 0003).
 */
final class BodyLimitFilter extends OncePerRequestFilter {

	private final int maxBytes;

	BodyLimitFilter(int maxBytes) {
		this.maxBytes = maxBytes;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (request.getContentLengthLong() > maxBytes) {
			response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
			return;
		}
		byte[] body = request.getInputStream().readNBytes(maxBytes + 1);
		if (body.length > maxBytes) {
			response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
			return;
		}
		chain.doFilter(new BufferedBodyRequest(request, body), response);
	}

	/** The request with its already-read body, readable once more. */
	private static final class BufferedBodyRequest extends HttpServletRequestWrapper {

		private final byte[] body;

		BufferedBodyRequest(HttpServletRequest request, byte[] body) {
			super(request);
			this.body = body;
		}

		@Override
		public ServletInputStream getInputStream() {
			var in = new ByteArrayInputStream(this.body);
			return new ServletInputStream() {

				@Override
				public int read() {
					return in.read();
				}

				@Override
				public int read(byte[] b, int off, int len) {
					return in.read(b, off, len);
				}

				@Override
				public boolean isFinished() {
					return in.available() == 0;
				}

				@Override
				public boolean isReady() {
					return true;
				}

				@Override
				public void setReadListener(ReadListener listener) {
					throw new UnsupportedOperationException();
				}

			};
		}

		@Override
		public BufferedReader getReader() throws IOException {
			String encoding = getCharacterEncoding();
			var charset = encoding == null ? StandardCharsets.ISO_8859_1
					: Charset.forName(encoding);
			return new BufferedReader(new InputStreamReader(getInputStream(), charset));
		}

	}

}
