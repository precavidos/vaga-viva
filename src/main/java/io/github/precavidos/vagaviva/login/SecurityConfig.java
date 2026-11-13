package io.github.precavidos.vagaviva.login;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Students sign in with GitHub (the Vaga Viva GitHub App's OAuth) only to
 * create their repository; reading a posting and planning stay open and
 * sessionless. The access token lives in the HTTP session, in memory, and is
 * never stored or logged (ADR 0003).
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

	/** Where the page resumes after GitHub sends the student back. */
	static final String AFTER_LOGIN = "/?entrou=1";

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		var api = PathPatternRequestMatcher.withDefaults();
		return http
			.authorizeHttpRequests((requests) -> requests
				// The invite page is open to anyone holding the join code.
				.requestMatchers(api.matcher(HttpMethod.GET, "/api/classrooms/{joinCode}"))
				.permitAll()
				.requestMatchers(api.matcher("/api/me"), api.matcher("/api/student-repositories/**"),
						api.matcher("/api/classrooms/**"), api.matcher("/api/weekly-email"))
				.authenticated()
				.anyRequest()
				.permitAll())
			.oauth2Login((login) -> login.defaultSuccessUrl(AFTER_LOGIN, true))
			.logout((logout) -> logout.logoutSuccessUrl("/"))
			// The session-backed calls need CSRF protection; the open ones use no session.
			.csrf((csrf) -> csrf.spa()
				.ignoringRequestMatchers(api.matcher("/api/job-postings/**"), api.matcher("/api/project-plans"),
						// GitHub signs its webhooks instead (X-Hub-Signature-256).
						api.matcher("/api/github/webhook"),
						// Resend signs its webhooks instead (Svix).
						api.matcher("/api/inbound-email"),
						// The unsubscribe token is the credential.
						api.matcher("/api/weekly-email/unsubscribe")))
			.exceptionHandling((exceptions) -> exceptions.defaultAuthenticationEntryPointFor(
					new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), api.matcher("/api/**")))
			.build();
	}

}
