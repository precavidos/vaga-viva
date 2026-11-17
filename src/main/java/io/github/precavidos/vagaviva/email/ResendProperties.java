package io.github.precavidos.vagaviva.email;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the Resend account that sends and receives the e-mails. All
 * come from the environment; blank values turn the e-mail features off.
 *
 * @param apiKey the Resend API key
 * @param webhookSecret the secret Resend (Svix) signs every inbound webhook with
 * @param from the sender address of every e-mail the app sends
 * @param publicUrl the app's public base URL, used in the links of the replies
 * @param inboundPerSenderPerHour how many replies one sender gets per hour
 * @param inboundPerDay how many replies the app sends per day (UTC)
 */
@ConfigurationProperties("vagaviva.resend")
public record ResendProperties(@DefaultValue("") String apiKey, @DefaultValue("") String webhookSecret,
		@DefaultValue("") String from, @DefaultValue("") String publicUrl,
		@DefaultValue("5") int inboundPerSenderPerHour, @DefaultValue("80") int inboundPerDay) {

	public ResendProperties {
		apiKey = apiKey == null ? "" : apiKey.strip();
		webhookSecret = webhookSecret == null ? "" : webhookSecret.strip();
		from = from == null ? "" : from.strip();
		publicUrl = publicUrl == null ? "" : publicUrl.strip();
		if (publicUrl.endsWith("/")) {
			publicUrl = publicUrl.substring(0, publicUrl.length() - 1);
		}
	}

	public boolean sendingEnabled() {
		return !apiKey.isBlank() && !from.isBlank() && !publicUrl.isBlank();
	}

	public boolean inboundEnabled() {
		return sendingEnabled() && !webhookSecret.isBlank();
	}

	/** Never shows the key or the secret. */
	@Override
	public String toString() {
		return "ResendProperties[from=" + from + ", publicUrl=" + publicUrl + ", apiKey=" + (apiKey.isBlank() ? "<blank>" : "****")
				+ ", webhookSecret=" + (webhookSecret.isBlank() ? "<blank>" : "****") + "]";
	}

}
