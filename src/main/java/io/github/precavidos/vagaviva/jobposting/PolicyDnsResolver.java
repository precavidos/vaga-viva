package io.github.precavidos.vagaviva.jobposting;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.apache.hc.client5.http.DnsResolver;

/**
 * DNS resolver for the HTTP client that applies an {@link AddressPolicy}. The
 * check lives here, and not before the request, because the client connects to
 * exactly the addresses this resolver returns: a host that answers with a
 * public address to a first lookup and a private one to a second (DNS
 * rebinding) cannot slip through, and every redirect is checked the same way.
 * A host is refused when any of its addresses is blocked.
 */
final class PolicyDnsResolver implements DnsResolver {

	@FunctionalInterface
	interface Lookup {

		InetAddress[] lookup(String host) throws UnknownHostException;

	}

	private final AddressPolicy policy;

	private final Lookup lookup;

	PolicyDnsResolver(AddressPolicy policy) {
		this(policy, InetAddress::getAllByName);
	}

	PolicyDnsResolver(AddressPolicy policy, Lookup lookup) {
		this.policy = policy;
		this.lookup = lookup;
	}

	@Override
	public InetAddress[] resolve(String host) throws UnknownHostException {
		InetAddress[] addresses = lookup.lookup(host);
		if (addresses == null || addresses.length == 0) {
			throw new UnknownHostException();
		}
		for (InetAddress address : addresses) {
			if (!policy.allows(address)) {
				throw new BlockedAddressException();
			}
		}
		return addresses;
	}

	@Override
	public List<InetSocketAddress> resolve(String host, int port) throws UnknownHostException {
		return Arrays.stream(resolve(host)).map(address -> new InetSocketAddress(address, port)).toList();
	}

	@Override
	public String resolveCanonicalHostname(String host) {
		return host;
	}

}
