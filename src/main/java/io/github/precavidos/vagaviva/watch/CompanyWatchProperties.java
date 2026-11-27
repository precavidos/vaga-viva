package io.github.precavidos.vagaviva.watch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the company watch.
 *
 * @param secret the shared secret the Sunday workflow sends; blank turns the whole feature off
 */
@ConfigurationProperties("vagaviva.company-watch")
record CompanyWatchProperties(@DefaultValue("") String secret) {

	CompanyWatchProperties {
		secret = secret == null ? "" : secret.strip();
	}

	/** Never shows the secret. */
	@Override
	public String toString() {
		return "CompanyWatchProperties[secret=" + (secret.isBlank() ? "<blank>" : "****") + "]";
	}

}
