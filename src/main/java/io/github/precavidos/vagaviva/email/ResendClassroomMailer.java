package io.github.precavidos.vagaviva.email;

import java.util.Map;

import io.github.precavidos.vagaviva.classroom.ClassroomMailer;

/** Sends the classroom e-mails through Resend, with the same sender and public address as the other e-mails. */
class ResendClassroomMailer implements ClassroomMailer {

	private final ResendApi resend;

	private final ResendProperties properties;

	ResendClassroomMailer(ResendApi resend, ResendProperties properties) {
		this.resend = resend;
		this.properties = properties;
	}

	@Override
	public boolean enabled() {
		return properties.sendingEnabled();
	}

	@Override
	public String link(String path) {
		return properties.publicUrl() + path;
	}

	@Override
	public void send(String to, String subject, String text, String html, String idempotencyKey) {
		resend.send(new OutgoingEmail(to, subject, text, html, Map.of(), idempotencyKey));
	}

}
