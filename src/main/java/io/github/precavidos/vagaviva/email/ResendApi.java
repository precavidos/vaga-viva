package io.github.precavidos.vagaviva.email;

/** The two Resend calls Vaga Viva needs. */
public interface ResendApi {

	/** Fetches the content of an e-mail Resend received (the webhook carries only its id). */
	ReceivedEmail receivedEmail(String emailId);

	void send(OutgoingEmail email);

}
