package io.github.precavidos.vagaviva.classroom;

/**
 * Sends the e-mails of the classroom mode. The classroom package only builds the texts; the e-mail package
 * knows the provider and the app's public address.
 */
public interface ClassroomMailer {

	/** Whether e-mails can be sent at all; when false, {@link #send} must not be called. */
	boolean enabled();

	/**
	 * The absolute link to a page of the app, as the recipient opens it.
	 * @param path a path starting with {@code /}
	 */
	String link(String path);

	/**
	 * Sends one e-mail.
	 * @param idempotencyKey makes a retry of the same e-mail harmless
	 */
	void send(String to, String subject, String text, String html, String idempotencyKey);

}
