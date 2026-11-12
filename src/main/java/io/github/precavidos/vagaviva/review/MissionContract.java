package io.github.precavidos.vagaviva.review;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubFile;
import io.github.precavidos.vagaviva.plan.Mission;

/** The contract of a mission as the student reads it: {@code missoes/<id>.md}, or the mission's summary. */
final class MissionContract {

	private MissionContract() {
	}

	static String read(GitHubApi github, ReviewContext context, Mission mission) {
		return github.readFile(context.token(), context.repositoryFullName(), "missoes/" + mission.id() + ".md")
			.map(GitHubFile::content)
			.orElse(mission.summary());
	}

}
