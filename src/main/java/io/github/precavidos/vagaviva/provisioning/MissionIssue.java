package io.github.precavidos.vagaviva.provisioning;

/** The issue opened for one mission of the plan. */
public record MissionIssue(String missionId, int number, String htmlUrl) {
}
