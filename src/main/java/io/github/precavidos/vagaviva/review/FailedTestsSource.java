package io.github.precavidos.vagaviva.review;

import java.util.List;

import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;

/** Reads which of the plan's tests failed on a commit, from the workflow's JUnit report (#23). */
public interface FailedTestsSource {

	List<FailedTest> failedTests(String token, String repositoryFullName, String headSha, StudentPlan plan);

}
