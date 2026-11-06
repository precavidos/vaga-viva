package io.github.precavidos.vagaviva.database;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

/**
 * A libpq-style connection string ({@code postgresql://user:password@host/db?...}),
 * as Neon and most hosts hand out, split into what the JDBC driver expects: a
 * {@code jdbc:postgresql://} URL plus separate credentials. Error messages never
 * include the input, since it carries the password.
 */
record PostgresUrl(String jdbcUrl, String username, String password) {

	static boolean isLibpqStyle(String value) {
		return value != null && (value.startsWith("postgresql://") || value.startsWith("postgres://"));
	}

	static PostgresUrl parse(String value) {
		if (!isLibpqStyle(value)) {
			throw new IllegalArgumentException("Not a postgresql:// connection string");
		}
		URI uri;
		try {
			uri = URI.create(value);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Malformed postgresql:// connection string");
		}
		String userInfo = uri.getRawUserInfo();
		if (userInfo == null || !userInfo.contains(":")) {
			throw new IllegalArgumentException("postgresql:// connection string has no user and password");
		}
		int colon = userInfo.indexOf(':');
		String username = decode(userInfo.substring(0, colon));
		String password = decode(userInfo.substring(colon + 1));

		StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
		if (uri.getPort() != -1) {
			jdbc.append(':').append(uri.getPort());
		}
		jdbc.append(uri.getRawPath());
		String query = jdbcQuery(uri.getRawQuery());
		if (!query.isEmpty()) {
			jdbc.append('?').append(query);
		}
		return new PostgresUrl(jdbc.toString(), username, password);
	}

	/**
	 * Keeps the libpq options the JDBC driver understands under the same name
	 * ({@code sslmode} among them) and drops {@code channel_binding}: with the
	 * driver's own {@code channelBinding=require}, Neon resets the connection.
	 * TLS still applies through {@code sslmode}.
	 */
	private static String jdbcQuery(String rawQuery) {
		if (rawQuery == null) {
			return "";
		}
		StringJoiner kept = new StringJoiner("&");
		for (String option : rawQuery.split("&")) {
			if (!option.isEmpty() && !option.startsWith("channel_binding=")) {
				kept.add(option);
			}
		}
		return kept.toString();
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	@Override
	public String toString() {
		return "PostgresUrl[jdbcUrl=" + jdbcUrl + ", username=" + username + ", password=***]";
	}

}
