package io.github.precavidos.vagaviva.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Stand-ins until the reviewer (#24) and the hints (#27) land: each of
 * those replaces its bean here. They only log what
 * arrived, never the diff or the token.
 */
@Configuration(proxyBeanMethods = false)
class PendingReviewSteps {

	private static final Logger log = LoggerFactory.getLogger(PendingReviewSteps.class);

	@Bean
	PullRequestReviewer pullRequestReviewer() {
		return (context) -> log.info("Review requested: {}", context);
	}

	@Bean
	HintResponder hintResponder() {
		return (context, request) -> log.info("Hint requested: {}", context);
	}

}
