package io.github.precavidos.vagaviva.review;

import java.util.Optional;

import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;

/**
 * What the weekly e-mail needs from a student's repository, read in memory.
 *
 * @param plan the plan in {@code .vaga-viva/plan.json}
 * @param report the latest test report, when the workflow produced one
 * @param lastReview the reviewer's last comment on the latest pull request, without marker and footer
 */
public record RepositorySnapshot(StudentPlan plan, Optional<TestReport> report, Optional<String> lastReview) {
}
