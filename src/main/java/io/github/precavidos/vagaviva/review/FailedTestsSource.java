package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Optional;

import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;

/**
 * Reads which of the plan's tests failed on a commit, from the workflow's
 * JUnit report (#23). Empty when no finished run or readable report exists yet,
 * which is not the same as "no test failed".
 */
public interface FailedTestsSource {

	Optional<List<FailedTest>> failedTests(String token, String repositoryFullName, String headSha, StudentPlan plan);

}
