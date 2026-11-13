package io.github.precavidos.vagaviva.email;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ResendProperties.class)
class EmailConfig {

	@Bean
	ResendApi resendApi(RestClient.Builder builder, ResendProperties properties) {
		return new ResendClient(builder, properties);
	}

	@Bean
	InboundLimits inboundLimits(ResendProperties properties, Clock clock) {
		return new InboundLimits(properties.inboundPerSenderPerHour(), properties.inboundPerDay(), clock);
	}

}
