package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubComment;
import io.github.precavidos.vagaviva.github.GitHubFile;
import io.github.precavidos.vagaviva.github.GitHubPullRequest;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/** How webhook events become review contexts and reach the reviewer and the hints. */
class WebhookRoutingTests {

	private static final String REPO = "maria/vaga-viva-clinic-scheduling";

	private static final String PLAN = """
			{ "archetype": "java-spring", "domain": "clinic-scheduling",
			  "missions": ["patients-api", "persistence", "unknown-mission", "bug-hunt"] }""";

	private static final MissionCatalog CATALOG = MissionCatalog.fromClasspath("missions/java-spring.json",
			SkillVocabulary.fromClasspath("vocabulary/java-spring.json"));

	private final GitHubApi github = mock(GitHubApi.class);

	private final List<ReviewContext> reviews = new ArrayList<>();

	private final List<String> hintRequests = new ArrayList<>();

	private final List<ReviewContext> hintContexts = new ArrayList<>();

	private final List<String> calls = new ArrayList<>();

	private boolean tracked = true;

	private int reportReads;

	private boolean failTheFirstRead;

	private Optional<TestReport> report = Optional.of(new TestReport(Set.of("patients-api"),
			List.of(new FailedTest("patients-api", "createsAPatient", "esperava status 201"))));

	private WebhookDispatcher dispatcher;

	@BeforeEach
	void setUp() {
		FailedTestsSource failedTests = (token, repository, sha) -> {
			if (reportReads++ == 0 && failTheFirstRead) {
				throw new IllegalStateException("GitHub is down");
			}
			return report;
		};
		ReviewEvents events = new ReviewEvents() {

			@Override
			public boolean tracks(String repositoryFullName) {
				return tracked;
			}

			@Override
			public void activity(String repositoryFullName) {
				calls.add("activity " + repositoryFullName);
			}

			@Override
			public void testsRan(String repositoryFullName, Integer pullRequestNumber, TestReport testReport) {
				calls.add("testsRan " + repositoryFullName + " " + pullRequestNumber + " " + testReport.missionsRun());
			}

			@Override
			public void reviewerAsked(String repositoryFullName, int pullRequestNumber, String question) {
			}

			@Override
			public void hintGiven(String repositoryFullName, String missionId) {
			}

		};
		var app = new GitHubAppProperties("1", "key", "secret", "vaga-viva");
		var builder = new ReviewContextBuilder(github, List.of(CATALOG), failedTests, app);
		var tokens = new InstallationTokens(github, () -> "jwt", java.time.Clock.systemUTC()) {

			@Override
			String forInstallation(long installationId) {
				return "ghs_installation_" + installationId;
			}

		};
		dispatcher = new WebhookDispatcher(tokens, builder, github, reviews::add, (context, request) -> {
			hintContexts.add(context);
			hintRequests.add(request);
		}, failedTests, events);
		when(github.readFile("ghs_installation_99", REPO, ".vaga-viva/plan.json"))
			.thenReturn(Optional.of(new GitHubFile(PLAN, "sha")));
		when(github.pullRequestDiff("ghs_installation_99", REPO, 3)).thenReturn("diff --git a/x b/x");
		when(github.issueComments("ghs_installation_99", REPO, 3)).thenReturn(List.of(
				new GitHubComment(1, "maria", false, "/dica"), new GitHubComment(2, "vaga-viva[bot]", true, "Pergunta 1"),
				new GitHubComment(3, "other-app[bot]", true, "outro bot")));
	}

	@Test
	void reviewsThePullRequestWhenTheVagaVivaWorkflowCompletes() {
		dispatch("workflow_run", workflowRun("completed", "Vaga Viva", "[{\"number\": 3}]"));

		assertThat(reviews).singleElement().satisfies(context -> {
			assertThat(context.token()).isEqualTo("ghs_installation_99");
			assertThat(context.repositoryFullName()).isEqualTo(REPO);
			assertThat(context.pullRequestNumber()).isEqualTo(3);
			assertThat(context.headSha()).isEqualTo("abc123");
			assertThat(context.plan().archetype()).isEqualTo("java-spring");
			assertThat(context.plan().missions()).extracting(Mission::id)
				.containsExactly("patients-api", "persistence", "bug-hunt");
			assertThat(context.diff()).isEqualTo("diff --git a/x b/x");
			assertThat(context.testReportRead()).isTrue();
			assertThat(context.failedTests()).extracting(FailedTest::missionId).containsExactly("patients-api");
			assertThat(context.reviewerComments()).extracting(ReviewContext.PreviousComment::body)
				.containsExactly("Pergunta 1");
		});
	}

	@Test
	void ignoresOtherWorkflowsRunsInProgressAndRunsWithoutAPullRequest() {
		dispatch("workflow_run", workflowRun("completed", "CI", "[{\"number\": 3}]"));
		dispatch("workflow_run", workflowRun("requested", "Vaga Viva", "[{\"number\": 3}]"));
		dispatch("workflow_run", workflowRun("completed", "Vaga Viva", "[]"));

		assertThat(reviews).isEmpty();
	}

	@Test
	void ignoresRepositoriesWithoutAPlan() {
		when(github.readFile("ghs_installation_99", REPO, ".vaga-viva/plan.json")).thenReturn(Optional.empty());

		dispatch("workflow_run", workflowRun("completed", "Vaga Viva", "[{\"number\": 3}]"));

		assertThat(reviews).isEmpty();
		verify(github, never()).pullRequestDiff(anyString(), anyString(), anyInt());
	}

	@Test
	void ignoresPlansOfUnknownArchetypes() {
		when(github.readFile("ghs_installation_99", REPO, ".vaga-viva/plan.json"))
			.thenReturn(Optional.of(new GitHubFile("{\"archetype\": \"cobol\", \"missions\": []}", "sha")));

		dispatch("workflow_run", workflowRun("completed", "Vaga Viva", "[{\"number\": 3}]"));

		assertThat(reviews).isEmpty();
	}

	@Test
	void answersAHintRequestOnAPullRequest() {
		when(github.pullRequest("ghs_installation_99", REPO, 3)).thenReturn(new GitHubPullRequest(3, "def456"));

		dispatch("issue_comment", issueComment("created", "/dica scheduling-rules", "User", true));

		assertThat(hintRequests).containsExactly("scheduling-rules");
		assertThat(hintContexts).singleElement().satisfies(context -> {
			assertThat(context.headSha()).isEqualTo("def456");
			assertThat(context.pullRequestNumber()).isEqualTo(3);
		});
	}

	@Test
	void ignoresCommentsThatAreNotHintRequestsFromAPersonOnAPullRequest() {
		dispatch("issue_comment", issueComment("created", "obrigado!", "User", true));
		dispatch("issue_comment", issueComment("created", "/dicas", "User", true));
		dispatch("issue_comment", issueComment("created", "/dica", "Bot", true));
		dispatch("issue_comment", issueComment("created", "/dica", "User", false));
		dispatch("issue_comment", issueComment("edited", "/dica", "User", true));

		assertThat(hintRequests).isEmpty();
		verify(github, never()).pullRequest(anyString(), anyString(), anyInt());
	}

	@Test
	void capsAVeryLargeDiff() {
		when(github.pullRequestDiff("ghs_installation_99", REPO, 3)).thenReturn("x".repeat(100_000));

		dispatch("workflow_run", workflowRun("completed", "Vaga Viva", "[{\"number\": 3}]"));

		assertThat(reviews.getFirst().diff()).hasSizeLessThan(ReviewContextBuilder.MAX_DIFF_LENGTH + 100)
			.endsWith("caracteres ...]\n");
	}

	@Test
	void ignoresUnreadableBodiesAndOtherEvents() {
		dispatcher.dispatch("workflow_run", "not json".getBytes(StandardCharsets.UTF_8));
		dispatch("installation", "{\"action\": \"created\"}");

		assertThat(reviews).isEmpty();
	}

	@Test
	void recordsTheTestsOfAPullRequestAndStillReviewsIt() {
		dispatch("workflow_run", workflowRun("completed", "Vaga Viva", "[{\"number\": 3}]"));

		assertThat(calls).containsExactly("testsRan " + REPO + " 3 [patients-api]");
		assertThat(reviews).hasSize(1);
	}

	@Test
	void recordsTestsOnTheDefaultBranchWithoutAPullRequest() {
		dispatch("workflow_run", workflowRun("completed", "Vaga Viva", "[]", "main"));

		assertThat(calls).containsExactly("testsRan " + REPO + " null [patients-api]");
		assertThat(reviews).isEmpty();
	}

	@Test
	void recordsOnlyActivityWhenThereIsNoReadableReport() {
		report = Optional.empty();

		dispatch("workflow_run", workflowRun("completed", "Vaga Viva", "[]", "main"));

		assertThat(calls).containsExactly("activity " + REPO);
	}

	@Test
	void ignoresPushesOnOtherBranchesWithoutAPullRequest() {
		dispatch("workflow_run", workflowRun("completed", "Vaga Viva", "[]", "feature"));

		assertThat(calls).isEmpty();
		assertThat(reportReads).isZero();
	}

	@Test
	void doesNotReadTheReportForUntrackedRepositories() {
		tracked = false;

		dispatch("workflow_run", workflowRun("completed", "Vaga Viva", "[]", "main"));

		assertThat(calls).isEmpty();
		assertThat(reportReads).isZero();
		verify(github, never()).workflowRuns(anyString(), anyString(), anyString());
	}

	@Test
	void aFailureWhileReadingTheReportDoesNotStopTheReview() {
		failTheFirstRead = true;

		dispatch("workflow_run", workflowRun("completed", "Vaga Viva", "[{\"number\": 3}]"));

		assertThat(reviews).hasSize(1);
	}

	@Test
	void aHintCommandCountsAsActivity() {
		when(github.pullRequest("ghs_installation_99", REPO, 3)).thenReturn(new GitHubPullRequest(3, "def456"));

		dispatch("issue_comment", issueComment("created", "/dica", "User", true));

		assertThat(calls).containsExactly("activity " + REPO);
	}

	private void dispatch(String event, String body) {
		dispatcher.dispatch(event, body.getBytes(StandardCharsets.UTF_8));
	}

	private static String workflowRun(String action, String name, String pullRequests) {
		return workflowRun(action, name, pullRequests, "feature");
	}

	private static String workflowRun(String action, String name, String pullRequests, String headBranch) {
		return """
				{ "action": "%s", "installation": { "id": 99 },
				  "repository": { "full_name": "%s", "default_branch": "main" },
				  "workflow_run": { "id": 5, "name": "%s", "head_sha": "abc123", "head_branch": "%s",
				                    "pull_requests": %s } }"""
			.formatted(action, REPO, name, headBranch, pullRequests);
	}

	private static String issueComment(String action, String body, String userType, boolean onPullRequest) {
		String pullRequest = onPullRequest ? ", \"pull_request\": { \"url\": \"x\" }" : "";
		return """
				{ "action": "%s", "installation": { "id": 99 }, "repository": { "full_name": "%s" },
				  "issue": { "number": 3%s },
				  "comment": { "body": "%s", "user": { "login": "maria", "type": "%s" } } }"""
			.formatted(action, REPO, pullRequest, body, userType);
	}

}
