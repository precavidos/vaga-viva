package io.github.precavidos.vagaviva.jobposting;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AddressPolicyTests {

	@ParameterizedTest
	@ValueSource(strings = { "127.0.0.1", "127.255.0.1", "10.1.2.3", "172.16.0.1", "172.31.255.255", "192.168.1.1",
			"169.254.169.254", "100.64.0.1", "100.127.255.255", "0.0.0.0", "0.1.2.3", "224.0.0.1", "::", "::1",
			"fe80::1", "fd00::1", "fc00::1", "ff02::1", "::ffff:127.0.0.1" })
	void blocksNonPublicAddresses(String address) throws UnknownHostException {
		assertThat(AddressPolicy.PUBLIC_ONLY.allows(InetAddress.getByName(address))).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "8.8.8.8", "172.32.0.1", "100.128.0.1", "2001:4860:4860::8888" })
	void allowsPublicAddresses(String address) throws UnknownHostException {
		assertThat(AddressPolicy.PUBLIC_ONLY.allows(InetAddress.getByName(address))).isTrue();
	}

}
