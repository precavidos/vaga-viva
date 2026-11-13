package io.github.precavidos.vagaviva.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the weekly e-mail trigger.
 *
 * @param secret the shared secret the scheduled workflow sends; blank turns the trigger off
 */
@ConfigurationProperties("vagaviva.weekly-email")
public record WeeklyEmailProperties(@DefaultValue("") String secret) {

	public WeeklyEmailProperties {
		secret = secret == null ? "" : secret.strip();
	}

	/** Never shows the secret. */
	@Override
	public String toString() {
		return "WeeklyEmailProperties[secret=" + (secret.isBlank() ? "<blank>" : "****") + "]";
	}

}
