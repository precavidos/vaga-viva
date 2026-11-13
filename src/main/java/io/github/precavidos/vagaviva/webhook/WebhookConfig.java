package io.github.precavidos.vagaviva.webhook;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Puts the body limit in front of the two anonymous webhooks, and only them. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebhookProperties.class)
class WebhookConfig {

	@Bean
	FilterRegistrationBean<BodyLimitFilter> webhookBodyLimit(WebhookProperties properties) {
		var registration = new FilterRegistrationBean<>(new BodyLimitFilter((int) properties.maxBodySize().toBytes()));
		// Exact paths: the servlet container would also match a "/api/github/webhook/" suffix, the controllers do not.
		registration.addUrlPatterns("/api/github/webhook", "/api/inbound-email");
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

}
