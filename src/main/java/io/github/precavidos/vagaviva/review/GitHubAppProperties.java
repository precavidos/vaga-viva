package io.github.precavidos.vagaviva.review;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The Vaga Viva GitHub App's own credentials, used for webhooks and for
 * acting as the app on students' repositories. All come from the
 * environment; blank values turn the webhook off.
 *
 * @param id the app id, the JWT issuer
 * @param privateKey the app private key in PEM, as GitHub hands it out
 * (PKCS#1) or as PKCS#8; {@code \n} escapes are accepted
 * @param webhookSecret the secret GitHub signs every delivery with
 * @param slug the app slug; its comments come from {@code <slug>[bot]}
 */
@ConfigurationProperties("vagaviva.github.app")
public record GitHubAppProperties(@DefaultValue("") String id, @DefaultValue("") String privateKey,
		@DefaultValue("") String webhookSecret, @DefaultValue("vaga-viva") String slug) {

	boolean webhookEnabled() {
		return !webhookSecret.isBlank() && !id.isBlank() && !privateKey.isBlank();
	}

	String botLogin() {
		return slug + "[bot]";
	}

	/** Never shows the key or the secret. */
	@Override
	public String toString() {
		return "GitHubAppProperties[id=" + id + ", slug=" + slug + ", privateKey=" + (privateKey.isBlank() ? "<blank>" : "****")
				+ ", webhookSecret=" + (webhookSecret.isBlank() ? "<blank>" : "****") + "]";
	}

}
