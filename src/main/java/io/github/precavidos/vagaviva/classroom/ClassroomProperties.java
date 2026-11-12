package io.github.precavidos.vagaviva.classroom;

import java.time.Duration;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the classroom mode.
 *
 * @param stuckAfter time on the same mission after which a student counts as stuck
 * @param idleAfter time without activity after which a student counts as idle
 * @param teachers the GitHub logins allowed to create classrooms; blank allows nobody
 * @param summarySecret the secret that protects the weekly summary endpoint; blank turns it off
 */
@ConfigurationProperties("vagaviva.classroom")
public record ClassroomProperties(@DefaultValue("3d") Duration stuckAfter, @DefaultValue("5d") Duration idleAfter,
		@DefaultValue Set<String> teachers, @DefaultValue("") String summarySecret) {

	/** Never shows the secret. */
	@Override
	public String toString() {
		return "ClassroomProperties[stuckAfter=" + stuckAfter + ", idleAfter=" + idleAfter + ", teachers=" + teachers
				+ ", summarySecret=" + (summarySecret.isBlank() ? "<blank>" : "****") + "]";
	}

}
