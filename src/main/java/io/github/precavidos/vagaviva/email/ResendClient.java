package io.github.precavidos.vagaviva.email;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

/**
 * Talks to the Resend API. The key is read per call, so a blank key (e-mail
 * off) never breaks startup. Errors carry only the status.
 */
class ResendClient implements ResendApi {

	private final RestClient http;

	private final ResendProperties properties;

	ResendClient(RestClient.Builder builder, ResendProperties properties) {
		this.properties = properties;
		this.http = builder.baseUrl("https://api.resend.com")
			.defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
				throw new ResendApiException(response.getStatusCode().value());
			})
			.build();
	}

	@Override
	public ReceivedEmail receivedEmail(String emailId) {
		JsonNode json;
		try {
			json = http.get()
				.uri("/emails/receiving/{id}", emailId)
				.headers(h -> h.setBearerAuth(properties.apiKey()))
				.retrieve()
				.body(JsonNode.class);
		}
		catch (RestClientException e) {
			// Messages can carry the e-mail content (ADR 0003); keep only the status.
			throw new ResendApiException(0);
		}
		if (json == null) {
			json = tools.jackson.databind.node.JsonNodeFactory.instance.objectNode();
		}
		Map<String, String> headers = new LinkedHashMap<>();
		for (var entry : json.path("headers").properties()) {
			headers.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().asString(""));
		}
		return new ReceivedEmail(json.path("from").asString(""), json.path("subject").asString(""),
				json.path("message_id").asString(""), json.path("text").asString(""), json.path("html").asString(""),
				Map.copyOf(headers));
	}

	@Override
	public void send(OutgoingEmail email) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("from", properties.from());
		body.put("to", List.of(email.to()));
		body.put("subject", email.subject());
		body.put("text", email.text());
		body.put("html", email.html());
		if (email.headers() != null && !email.headers().isEmpty()) {
			body.put("headers", email.headers());
		}
		try {
			http.post()
				.uri("/emails")
				.headers(h -> {
					h.setBearerAuth(properties.apiKey());
					if (email.idempotencyKey() != null && !email.idempotencyKey().isBlank()) {
						h.set("Idempotency-Key", email.idempotencyKey());
					}
				})
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.toBodilessEntity();
		}
		catch (RestClientException e) {
			throw new ResendApiException(0);
		}
	}

	/** Never shows the key. */
	@Override
	public String toString() {
		return "ResendClient[from=" + properties.from() + "]";
	}

}
