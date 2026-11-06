package io.github.precavidos.vagaviva.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class PostgresUrlTests {

	@Test
	void splitsNeonStyleUrlIntoJdbcUrlAndCredentials() {
		var url = PostgresUrl.parse(
				"postgresql://vagaviva_owner:s3cr%40t@ep-cool-1-pooler.us-east-1.aws.neon.tech/vagaviva?sslmode=require&channel_binding=require");

		assertThat(url.jdbcUrl()).isEqualTo(
				"jdbc:postgresql://ep-cool-1-pooler.us-east-1.aws.neon.tech/vagaviva?sslmode=require");
		assertThat(url.username()).isEqualTo("vagaviva_owner");
		assertThat(url.password()).isEqualTo("s3cr@t");
	}

	@Test
	void acceptsPostgresSchemeAndPort() {
		var url = PostgresUrl.parse("postgres://app:pw@localhost:5432/vagaviva");

		assertThat(url.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/vagaviva");
		assertThat(url.username()).isEqualTo("app");
		assertThat(url.password()).isEqualTo("pw");
	}

	@Test
	void recognisesOnlyLibpqStyleUrls() {
		assertThat(PostgresUrl.isLibpqStyle("postgresql://a:b@h/db")).isTrue();
		assertThat(PostgresUrl.isLibpqStyle("postgres://a:b@h/db")).isTrue();
		assertThat(PostgresUrl.isLibpqStyle("jdbc:postgresql://h/db")).isFalse();
		assertThat(PostgresUrl.isLibpqStyle(null)).isFalse();
	}

	@Test
	void rejectsUrlWithoutCredentials() {
		assertThatIllegalArgumentException().isThrownBy(() -> PostgresUrl.parse("postgresql://host/db"))
			.withMessageNotContaining("host/db");
	}

}
