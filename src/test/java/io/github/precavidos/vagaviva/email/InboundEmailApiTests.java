package io.github.precavidos.vagaviva.email;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;

/**
 * Resend reaches the endpoint with no session, login or CSRF token: only the Svix signature counts. The limits
 * are shared by the cached Spring context, so every test uses its own sender and its own event id.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "vagaviva.resend.api-key=re_test",
		"vagaviva.resend.webhook-secret=" + SvixSignatureTests.SECRET,
		"vagaviva.resend.from=Vaga Viva <vaga@vagaviva.dev>", "vagaviva.resend.public-url=https://vagaviva.dev" })
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class InboundEmailApiTests {

	@Autowired
	MockMvcTester mvc;

	@MockitoBean
	ResendApi resend;

	@Test
	void repliesWithThePlan() throws IOException {
		when(resend.receivedEmail("e-1")).thenReturn(email("maria-plan@example.com", "Fwd: Vaga Java", fixture("gmail-pt.txt"), Map.of()));

		var response = post("evt-1", event("e-1", "maria-plan@example.com"));

		assertThat(response).hasStatus(HttpStatus.OK);
		var sent = sent();
		assertThat(sent.to()).isEqualTo("maria-plan@example.com");
		assertThat(sent.subject()).isEqualTo("Re: Fwd: Vaga Java");
		assertThat(sent.text()).contains("https://vagaviva.dev/#vaga=");
		assertThat(sent.headers()).containsEntry("In-Reply-To", "<m1@x>")
			.containsEntry("References", "<m1@x>")
			.containsEntry("Auto-Submitted", "auto-replied");
		assertThat(sent.idempotencyKey()).isEqualTo("inbound/evt-1");
	}

	@Test
	void longPostingRepliesWithoutLink() {
		var body = new Random(7).ints(20_000, 'a', 'z' + 1).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
		when(resend.receivedEmail("e-long")).thenReturn(email("long@example.com", "Fwd: vaga", body, Map.of()));

		var response = post("evt-long", event("e-long", "long@example.com"));

		assertThat(response).hasStatus(HttpStatus.OK);
		assertThat(sent().text()).doesNotContain("#vaga=");
	}

	@Test
	void shortPostingGetsHelp() {
		when(resend.receivedEmail("e-short")).thenReturn(email("short@example.com", "Fwd: vaga", "x".repeat(50), Map.of()));

		var response = post("evt-short", event("e-short", "short@example.com"));

		assertThat(response).hasStatus(HttpStatus.OK);
		assertThat(sent().text()).isEqualTo(PlanReplyTemplate.help("https://vagaviva.dev", "Fwd: vaga").text());
	}

	@Test
	void ignoresOtherEventTypes() {
		var response = post("evt-other", "{\"type\":\"email.sent\"}");

		assertThat(response).hasStatus(HttpStatus.OK);
		verifyNoInteractions(resend);
	}

	@Test
	void rejectsABadSignature() {
		var response = mvc.post()
			.uri("/api/inbound-email")
			.header("svix-id", "evt-bad")
			.header("svix-timestamp", String.valueOf(Instant.now().getEpochSecond()))
			.header("svix-signature", "v1,AAAA")
			.contentType(MediaType.APPLICATION_JSON)
			.content(event("e-bad", "bad@example.com"))
			.exchange();

		assertThat(response).hasStatus(HttpStatus.UNAUTHORIZED);
		verifyNoInteractions(resend);
	}

	@Test
	void processesARepeatedEventOnce() throws IOException {
		when(resend.receivedEmail("e-dup")).thenReturn(email("dup@example.com", "Fwd: vaga", fixture("gmail-pt.txt"), Map.of()));

		var first = post("evt-dup", event("e-dup", "dup@example.com"));
		var second = post("evt-dup", event("e-dup", "dup@example.com"));

		assertThat(first).hasStatus(HttpStatus.OK);
		assertThat(second).hasStatus(HttpStatus.OK);
		verify(resend, times(1)).send(any());
	}

	@Test
	void ignoresOurOwnAddress() throws IOException {
		assertIgnored("own", "vaga@vagaviva.dev", Map.of());
	}

	@Test
	void ignoresMailerDaemon() throws IOException {
		assertIgnored("daemon", "MAILER-DAEMON@x.com", Map.of());
	}

	@Test
	void ignoresAutoSubmitted() throws IOException {
		assertIgnored("auto", "auto@example.com", Map.of("auto-submitted", "auto-replied"));
	}

	@Test
	void answersAutoSubmittedNo() throws IOException {
		when(resend.receivedEmail("e-no")).thenReturn(
				email("no@example.com", "Fwd: vaga", fixture("gmail-pt.txt"), Map.of("auto-submitted", "no")));

		var response = post("evt-no", event("e-no", "no@example.com"));

		assertThat(response).hasStatus(HttpStatus.OK);
		verify(resend).send(any());
	}

	@Test
	void ignoresABlankOrMalformedSender() throws IOException {
		assertIgnored("blank", "", Map.of());
		assertIgnored("malformed", "not-an-address", Map.of());
	}

	@Test
	void givesTheQuotaBackWhenSendingFails() throws IOException {
		var body = fixture("gmail-pt.txt");
		doThrow(new ResendApiException(500)).when(resend).send(any());
		for (int i = 0; i < 5; i++) {
			when(resend.receivedEmail("e-ref" + i)).thenReturn(email("refund@example.com", "Fwd: vaga", body, Map.of()));
			assertThat(post("evt-ref" + i, event("e-ref" + i, "refund@example.com"))).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
		}
		doNothing().when(resend).send(any());
		when(resend.receivedEmail("e-ref5")).thenReturn(email("refund@example.com", "Fwd: vaga", body, Map.of()));

		assertThat(post("evt-ref5", event("e-ref5", "refund@example.com"))).hasStatus(HttpStatus.OK);

		verify(resend, times(6)).send(any());
		var captor = ArgumentCaptor.forClass(OutgoingEmail.class);
		verify(resend, times(6)).send(captor.capture());
		assertThat(captor.getAllValues().get(5).text()).contains("#vaga=");
	}

	@Test
	void limitsTheSender() throws IOException {
		var body = fixture("gmail-pt.txt");
		for (int i = 0; i < 6; i++) {
			when(resend.receivedEmail("e-lim" + i)).thenReturn(email("limit@example.com", "Fwd: vaga", body, Map.of()));
			assertThat(post("evt-lim" + i, event("e-lim" + i, "limit@example.com"))).hasStatus(HttpStatus.OK);
		}

		verify(resend, times(5)).send(any());
	}

	@Test
	void retriesWhenResendFails() throws IOException {
		when(resend.receivedEmail("e-retry")).thenThrow(new ResendApiException(500));
		assertThat(post("evt-retry", event("e-retry", "retry@example.com"))).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);

		doReturn(email("retry@example.com", "Fwd: vaga", fixture("gmail-pt.txt"), Map.of())).when(resend).receivedEmail("e-retry");
		assertThat(post("evt-retry", event("e-retry", "retry@example.com"))).hasStatus(HttpStatus.OK);

		verify(resend, times(1)).send(any());
	}

	@Test
	void retriesWhenSendingFails() throws IOException {
		when(resend.receivedEmail("e-sendfail")).thenReturn(email("sendfail@example.com", "Fwd: vaga", fixture("gmail-pt.txt"), Map.of()));
		doThrow(new ResendApiException(429)).when(resend).send(any());

		assertThat(post("evt-sendfail", event("e-sendfail", "sendfail@example.com"))).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void answersAnUnexpectedFailureWith500(CapturedOutput output) throws IOException {
		when(resend.receivedEmail("e-boom")).thenThrow(new IllegalStateException("maria-boom@example.com"));

		assertThat(post("evt-boom", event("e-boom", "boom@example.com"))).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(output.getAll()).doesNotContain("maria-boom@example.com");
	}

	@Test
	void logsNeitherSenderNorBody(CapturedOutput output) throws IOException {
		when(resend.receivedEmail("e-log1")).thenReturn(email("maria@example.com", "Fwd: vaga", fixture("gmail-pt.txt"), Map.of()));
		assertThat(post("evt-log1", event("e-log1", "maria@example.com"))).hasStatus(HttpStatus.OK);

		when(resend.receivedEmail("e-log2")).thenReturn(email("maria2@example.com", "Fwd: vaga", fixture("gmail-pt.txt"), Map.of()));
		doThrow(new ResendApiException(500)).when(resend).send(any());
		assertThat(post("evt-log2", event("e-log2", "maria2@example.com"))).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);

		assertThat(output.getAll()).contains("evt-log1", "evt-log2")
			.doesNotContain("maria@example.com", "maria2@example.com", "Desenvolvedora Java", "#vaga=");
	}

	private void assertIgnored(String id, String from, Map<String, String> headers) throws IOException {
		when(resend.receivedEmail("e-" + id)).thenReturn(email(from, "Fwd: vaga", fixture("gmail-pt.txt"), headers));

		var response = post("evt-" + id, event("e-" + id, from));

		assertThat(response).hasStatus(HttpStatus.OK);
		verify(resend, never()).send(any());
	}

	private OutgoingEmail sent() {
		var captor = ArgumentCaptor.forClass(OutgoingEmail.class);
		verify(resend).send(captor.capture());
		return captor.getValue();
	}

	private MvcTestResult post(String svixId, String json) {
		return signed(mvc.post().uri("/api/inbound-email"), svixId, json).exchange();
	}

	private static MockMvcRequestBuilder signed(MockMvcRequestBuilder request, String svixId, String json) {
		var timestamp = String.valueOf(Instant.now().getEpochSecond());
		return request.header("svix-id", svixId)
			.header("svix-timestamp", timestamp)
			.header("svix-signature", "v1," + sign(svixId, timestamp, json))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json);
	}

	private static String sign(String id, String timestamp, String json) {
		try {
			var key = Base64.getDecoder().decode(SvixSignatureTests.SECRET.substring("whsec_".length()));
			var mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			return Base64.getEncoder().encodeToString(mac.doFinal((id + "." + timestamp + "." + json).getBytes(UTF_8)));
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static String event(String emailId, String from) {
		return "{\"type\":\"email.received\",\"data\":{\"email_id\":\"" + emailId + "\",\"from\":\"" + from
				+ "\",\"subject\":\"Fwd: Vaga Java\"}}";
	}

	private static ReceivedEmail email(String from, String subject, String text, Map<String, String> headers) {
		return new ReceivedEmail(from, subject, "<m1@x>", text, "", headers);
	}

	private static String fixture(String name) throws IOException {
		return new String(new ClassPathResource("email/" + name).getInputStream().readAllBytes(), UTF_8);
	}

}
