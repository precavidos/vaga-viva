package io.github.precavidos.vagaviva.jobposting;

import java.net.UnknownHostException;

/**
 * Thrown when a host resolves to an address that {@link AddressPolicy} does not
 * allow. It is an {@link UnknownHostException} so the HTTP client treats it as
 * a failed lookup and never connects. The message names neither the host nor
 * the address (ADR 0003).
 */
final class BlockedAddressException extends UnknownHostException {

	BlockedAddressException() {
		super("blocked address");
	}

}
