package io.github.precavidos.vagaviva.classroom;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the classroom mode.
 *
 * @param stuckAfter time on the same mission after which a student counts as stuck
 * @param idleAfter time without activity after which a student counts as idle
 * @param teachers the GitHub logins allowed to create classrooms; blank allows nobody; stored trimmed and in lower case, because GitHub logins ignore case
 * @param summarySecret the secret that protects the weekly summary endpoint; blank turns it off
 */
@ConfigurationProperties("vagaviva.classroom")
public record ClassroomProperties(@DefaultValue("3d") Duration stuckAfter, @DefaultValue("5d") Duration idleAfter,
		@DefaultValue Set<String> teachers, @DefaultValue("") String summarySecret) {

	public ClassroomProperties {
		teachers = teachers.stream()
			.map((login) -> login.strip().toLowerCase(Locale.ROOT))
			.filter((login) -> !login.isEmpty())
			.collect(Collectors.toUnmodifiableSet());
	}

	/** Never shows the secret. */
	@Override
	public String toString() {
		return "ClassroomProperties[stuckAfter=" + stuckAfter + ", idleAfter=" + idleAfter + ", teachers=" + teachers
				+ ", summarySecret=" + (summarySecret.isBlank() ? "<blank>" : "****") + "]";
	}

}
