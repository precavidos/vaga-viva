package io.github.precavidos.vagaviva.email;

import java.time.Clock;
import java.time.Duration;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import io.github.precavidos.vagaviva.classroom.ClassroomStandings;
import io.github.precavidos.vagaviva.review.RepositorySnapshots;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ResendProperties.class)
class EmailConfig {

	@Bean
	ResendApi resendApi(RestClient.Builder builder, ResendProperties properties) {
		var settings = HttpClientSettings.defaults()
			.withConnectTimeout(Duration.ofSeconds(5))
			.withReadTimeout(Duration.ofSeconds(10));
		return new ResendClient(
				builder.clone().requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings)), properties);
	}

	@Bean
	InboundLimits inboundLimits(ResendProperties properties, Clock clock) {
		return new InboundLimits(properties.inboundPerSenderPerHour(), properties.inboundPerDay(), clock);
	}

	@Bean
	WeeklyEmailService weeklyEmailService(WeeklyEmailSubscriptionRepository subscriptions,
			RepositorySnapshots snapshots, ClassroomStandings standings, ResendApi resend,
			ResendProperties properties, Clock clock) {
		return new WeeklyEmailService(subscriptions, snapshots, standings, resend, properties, clock,
				WeeklyEmailService.Pause.SLEEP);
	}

}
