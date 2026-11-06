package io.github.precavidos.vagaviva.writing;

import io.github.precavidos.vagaviva.plan.ProjectPlan;

/**
 * Writes the student-facing texts for a plan: a project introduction and a
 * context for each mission. It never changes the plan (ADR 0002). Implementations
 * never throw: when they cannot write a text, they leave it out and the default
 * text is used instead.
 */
public interface MissionTextWriter {

	WrittenTexts write(ProjectPlan plan);

}
