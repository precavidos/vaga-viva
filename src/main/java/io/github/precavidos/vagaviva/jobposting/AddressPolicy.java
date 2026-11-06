package io.github.precavidos.vagaviva.jobposting;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

/**
 * Decides which network addresses the server may connect to when it downloads
 * a career page. Pages come from URLs that users type, so without this check
 * a URL could make the server reach its own network (SSRF).
 */
@FunctionalInterface
interface AddressPolicy {

	/**
	 * Only public internet addresses: no loopback, private, link-local (which
	 * includes cloud metadata at 169.254.169.254), carrier-grade NAT,
	 * "this network", reserved, multicast or IPv6 unique-local addresses.
	 * IPv4-mapped IPv6 addresses arrive here as IPv4 addresses.
	 */
	AddressPolicy PUBLIC_ONLY = address -> !(address.isAnyLocalAddress() || address.isLoopbackAddress()
			|| address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isMulticastAddress()
			|| isSpecialIpv4(address) || isUniqueLocalIpv6(address));

	boolean allows(InetAddress address);

	private static boolean isSpecialIpv4(InetAddress address) {
		if (!(address instanceof Inet4Address)) {
			return false;
		}
		byte[] b = address.getAddress();
		int first = b[0] & 0xFF;
		boolean thisNetwork = first == 0;
		boolean carrierGradeNat = first == 100 && (b[1] & 0xC0) == 64;
		boolean reserved = first >= 240;
		return thisNetwork || carrierGradeNat || reserved;
	}

	private static boolean isUniqueLocalIpv6(InetAddress address) {
		return address instanceof Inet6Address && (address.getAddress()[0] & 0xFE) == 0xFC;
	}

}
