package io.github.precavidos.vagaviva.review;

/**
 * What happened on a student's repository, told to whoever keeps track of the
 * student's progress (the classroom mode). The review package only emits the
 * events; it knows nothing about classrooms.
 * <p>
 * Implementations must never let a failure escape: recording progress is not
 * allowed to stop the reviewer or the hints.
 */
public interface ReviewEvents {

	/** Whether the repository belongs to a classroom. When it does not, nothing else is worth calling. */
	boolean tracks(String repositoryFullName);

	/** The student did something on the repository, but nothing that says where they are in the plan. */
	void activity(String repositoryFullName);

	/**
	 * The tests ran on the default branch or on a pull request.
	 * @param pullRequestNumber the pull request, or {@code null} for a push on the default branch
	 * @param report what the run reported
	 */
	void testsRan(String repositoryFullName, Integer pullRequestNumber, TestReport report);

	/** The reviewer posted {@code question} on the pull request. */
	void reviewerAsked(String repositoryFullName, int pullRequestNumber, String question);

	/** The student received a hint about {@code missionId}. */
	void hintGiven(String repositoryFullName, String missionId);

}
