package io.github.precavidos.vagaviva.email;

/**
 * A failed Resend call. Carries only the HTTP status (0 for an I/O failure):
 * the response and the cause can hold e-mail addresses, which ADR 0003 keeps
 * out of messages and logs.
 */
public class ResendApiException extends RuntimeException {

	private final int status;

	public ResendApiException(int status) {
		super("Resend answered " + status, null, false, false);
		this.status = status;
	}

	public int status() {
		return status;
	}

}
