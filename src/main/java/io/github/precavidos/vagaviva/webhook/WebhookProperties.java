package io.github.precavidos.vagaviva.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Settings shared by the anonymous webhooks (GitHub and Resend).
 *
 * @param maxBodySize the largest request body they accept; a bigger one gets a 413 before it is read
 */
@ConfigurationProperties("vagaviva.webhook")
public record WebhookProperties(@DefaultValue("1MB") DataSize maxBodySize) {

	public WebhookProperties {
		if (maxBodySize == null || maxBodySize.toBytes() < 1 || maxBodySize.toBytes() >= Integer.MAX_VALUE) {
			throw new IllegalArgumentException("vagaviva.webhook.max-body-size must be between 1 byte and 2 GB");
		}
	}

}
