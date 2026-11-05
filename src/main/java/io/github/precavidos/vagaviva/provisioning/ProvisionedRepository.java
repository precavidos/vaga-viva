package io.github.precavidos.vagaviva.provisioning;

import java.util.List;

/** A student repository created from the template, with one issue per planned mission. */
public record ProvisionedRepository(String fullName, String htmlUrl, List<MissionIssue> missionIssues) {

	public ProvisionedRepository {
		missionIssues = List.copyOf(missionIssues);
	}

}
