package io.github.precavidos.vagaviva.jobposting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

class PolicyDnsResolverTests {

	@Test
	void rejectsLocalhost() {
		var resolver = new PolicyDnsResolver(AddressPolicy.PUBLIC_ONLY);

		assertThatThrownBy(() -> resolver.resolve("localhost")).isInstanceOf(BlockedAddressException.class);
	}

	@Test
	void rejectsAHostWithAnyPrivateAddress() {
		var resolver = new PolicyDnsResolver(AddressPolicy.PUBLIC_ONLY,
				host -> new InetAddress[] { ip("8.8.8.8"), ip("10.0.0.1") });

		assertThatThrownBy(() -> resolver.resolve("vagas.exemplo.test")).isInstanceOf(BlockedAddressException.class);
		assertThatThrownBy(() -> resolver.resolve("vagas.exemplo.test", 443))
			.isInstanceOf(BlockedAddressException.class);
	}

	@Test
	void returnsAllAddressesOfAPublicHost() throws UnknownHostException {
		var resolver = new PolicyDnsResolver(AddressPolicy.PUBLIC_ONLY,
				host -> new InetAddress[] { ip("8.8.8.8"), ip("8.8.4.4") });

		assertThat(resolver.resolve("vagas.exemplo.test")).containsExactly(ip("8.8.8.8"), ip("8.8.4.4"));
		assertThat(resolver.resolve("vagas.exemplo.test", 443)).containsExactly(
				new InetSocketAddress(ip("8.8.8.8"), 443), new InetSocketAddress(ip("8.8.4.4"), 443));
	}

	@Test
	void blockedExceptionDoesNotNameTheHost() {
		var resolver = new PolicyDnsResolver(AddressPolicy.PUBLIC_ONLY, host -> new InetAddress[] { ip("10.0.0.1") });

		assertThatThrownBy(() -> resolver.resolve("segredo.interno"))
			.isInstanceOf(BlockedAddressException.class)
			.satisfies(e -> assertThat(String.valueOf(e.getMessage())).doesNotContain("segredo.interno")
				.doesNotContain("10.0.0.1"));
	}

	private static InetAddress ip(String literal) {
		try {
			return InetAddress.getByName(literal);
		}
		catch (UnknownHostException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
