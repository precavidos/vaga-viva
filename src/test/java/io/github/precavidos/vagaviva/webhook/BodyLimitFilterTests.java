package io.github.precavidos.vagaviva.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

class BodyLimitFilterTests {

	private final BodyLimitFilter filter = new BodyLimitFilter(10);

	private final AtomicReference<byte[]> seen = new AtomicReference<>();

	@Test
	void passesABodyWithinTheLimitOnUnchanged() throws Exception {
		var response = run(request(new byte[10]));

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(seen.get()).hasSize(10);
	}

	@Test
	void rejectsADeclaredLengthAboveTheLimitWithoutReadingTheBody() throws Exception {
		var request = request(new byte[11]);

		var response = run(request);

		assertThat(response.getStatus()).isEqualTo(413);
		assertThat(seen.get()).isNull();
	}

	@Test
	void stopsReadingABodyWithoutOrWithAWrongLength() throws Exception {
		assertThat(run(declaring(new byte[11], -1)).getStatus()).isEqualTo(413);
		assertThat(run(declaring(new byte[11], 3)).getStatus()).isEqualTo(413);
		assertThat(seen.get()).isNull();
	}

	@Test
	void letsAChunkedBodyWithinTheLimitThrough() throws Exception {
		var response = run(declaring("12345".getBytes(), -1));

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(seen.get()).isEqualTo("12345".getBytes());
	}

	/** A request that claims another length than its body has, or none (-1), as a chunked one does. */
	private static MockHttpServletRequest declaring(byte[] body, long length) {
		var request = new MockHttpServletRequest("POST", "/api/inbound-email") {

			@Override
			public long getContentLengthLong() {
				return length;
			}

		};
		request.setContent(body);
		return request;
	}

	private static MockHttpServletRequest request(byte[] body) {
		var request = new MockHttpServletRequest("POST", "/api/inbound-email");
		request.setContent(body);
		return request;
	}

	private MockHttpServletResponse run(MockHttpServletRequest request) throws ServletException, IOException {
		var response = new MockHttpServletResponse();
		filter.doFilter(request, response, (req, res) -> seen.set(req.getInputStream().readAllBytes()));
		return response;
	}

}
