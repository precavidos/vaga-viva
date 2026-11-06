package io.github.precavidos.vagaviva.writing;

import io.github.precavidos.vagaviva.plan.ProjectPlan;

/**
 * The writer used when no language model is configured: it writes nothing, so
 * every student gets the default Portuguese texts.
 */
public final class DefaultMissionTextWriter implements MissionTextWriter {

	@Override
	public WrittenTexts write(ProjectPlan plan) {
		return WrittenTexts.none();
	}

}
