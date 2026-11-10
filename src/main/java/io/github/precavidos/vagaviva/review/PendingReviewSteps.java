package io.github.precavidos.vagaviva.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Stand-in until the hints (#27) land and replace this bean. They only log what
 * arrived, never the diff or the token.
 */
@Configuration(proxyBeanMethods = false)
class PendingReviewSteps {

	private static final Logger log = LoggerFactory.getLogger(PendingReviewSteps.class);

	@Bean
	HintResponder hintResponder() {
		return (context, request) -> log.info("Hint requested: {}", context);
	}

}
