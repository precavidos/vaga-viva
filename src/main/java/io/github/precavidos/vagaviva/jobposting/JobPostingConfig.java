package io.github.precavidos.vagaviva.jobposting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FirecrawlProperties.class)
class JobPostingConfig {

	private static final Logger log = LoggerFactory.getLogger(JobPostingConfig.class);

	@Bean
	RenderedPageReader renderedPageReader(FirecrawlProperties firecrawl) {
		if (firecrawl.apiKey().isBlank()) {
			log.info("FIRECRAWL_API_KEY is blank: pages built by JavaScript stay unreadable");
			return RenderedPageReader.NONE;
		}
		return new FirecrawlPageReader(firecrawl);
	}

}
