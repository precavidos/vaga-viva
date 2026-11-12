package io.github.precavidos.vagaviva.email;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ResendProperties.class)
class EmailConfig {

}
