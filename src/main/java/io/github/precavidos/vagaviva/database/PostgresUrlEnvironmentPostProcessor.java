package io.github.precavidos.vagaviva.database;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Lets {@code DATABASE_URL} and {@code DATABASE_URL_UNPOOLED} be pasted as Neon
 * gives them ({@code postgresql://user:password@host/db}). The application uses
 * {@code DATABASE_URL} (Neon's pooled endpoint); Flyway uses
 * {@code DATABASE_URL_UNPOOLED} when present, because migrations need a direct
 * connection rather than PgBouncer in transaction mode. JDBC-style values
 * ({@code jdbc:postgresql://...} with {@code DATABASE_USERNAME} and
 * {@code DATABASE_PASSWORD}) keep working through {@code application.yaml}.
 */
class PostgresUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

	static final String PROPERTY_SOURCE_NAME = "postgresUrl";

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> properties = new HashMap<>();
		String pooled = environment.getProperty("DATABASE_URL");
		if (PostgresUrl.isLibpqStyle(pooled)) {
			put(properties, "spring.datasource", "username", PostgresUrl.parse(pooled));
		}
		String direct = environment.getProperty("DATABASE_URL_UNPOOLED");
		if (PostgresUrl.isLibpqStyle(direct)) {
			put(properties, "spring.flyway", "user", PostgresUrl.parse(direct));
		}
		if (!properties.isEmpty()) {
			// Ahead of application.yaml, whose spring.datasource.url would otherwise take the raw value.
			environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
		}
	}

	private static void put(Map<String, Object> properties, String prefix, String userKey, PostgresUrl url) {
		properties.put(prefix + ".url", url.jdbcUrl());
		properties.put(prefix + "." + userKey, url.username());
		properties.put(prefix + ".password", url.password());
	}

}
