package io.github.precavidos.vagaviva.review;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubPullRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Routes GitHub App webhook events to the reviewer and the hints:
 * <ul>
 * <li>{@code workflow_run} completed for the {@value #WORKFLOW} workflow on a
 * pull request: review it;</li>
 * <li>{@code issue_comment} created on a pull request by a person, starting
 * with {@value #HINT_COMMAND}: answer with a hint.</li>
 * </ul>
 * Everything else is ignored. Only the repository and the pull request number
 * are logged, never payload contents.
 */
@Component
class WebhookDispatcher {

	/** The workflow of the templates' {@code .github/workflows/vaga-viva.yml}. */
	static final String WORKFLOW = "Vaga Viva";

	static final String HINT_COMMAND = "/dica";

	private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final InstallationTokens tokens;

	private final ReviewContextBuilder contexts;

	private final GitHubApi github;

	private final PullRequestReviewer reviewer;

	private final HintResponder hints;

	private final FailedTestsSource failedTests;

	private final ReviewEvents events;

	WebhookDispatcher(InstallationTokens tokens, ReviewContextBuilder contexts, GitHubApi github,
			PullRequestReviewer reviewer, HintResponder hints, FailedTestsSource failedTests, ReviewEvents events) {
		this.tokens = tokens;
		this.contexts = contexts;
		this.github = github;
		this.reviewer = reviewer;
		this.hints = hints;
		this.failedTests = failedTests;
		this.events = events;
	}

	void dispatch(String event, byte[] body) {
		JsonNode payload;
		try {
			payload = JSON.readTree(body);
		}
		catch (JacksonException e) {
			log.warn("Ignoring a {} webhook with an unreadable body", event);
			return;
		}
		switch (event) {
			case "workflow_run" -> workflowRun(payload);
			case "issue_comment" -> issueComment(payload);
			default -> {
				// Installations, pings and the like need nothing.
			}
		}
	}

	private void workflowRun(JsonNode payload) {
		JsonNode run = payload.path("workflow_run");
		if (!"completed".equals(payload.path("action").asString("")) || !WORKFLOW.equals(run.path("name").asString(""))) {
			return;
		}
		String repository = payload.path("repository").path("full_name").asString("");
		long installationId = payload.path("installation").path("id").asLong();
		String headSha = run.path("head_sha").asString("");
		boolean hasPullRequest = !run.path("pull_requests").isEmpty();
		Integer number = hasPullRequest ? run.path("pull_requests").get(0).path("number").asInt() : null;
		String headBranch = run.path("head_branch").asString("");
		boolean onDefaultBranch = !headBranch.isEmpty()
				&& headBranch.equals(payload.path("repository").path("default_branch").asString(""));
		String headRepository = run.path("head_repository").path("full_name").asString("");
		boolean ownRepository = !headRepository.isEmpty() && headRepository.equalsIgnoreCase(repository);
		if (ownRepository && (hasPullRequest || onDefaultBranch)) {
			recordProgress(repository, number, installationId, headSha);
		}
		if (!hasPullRequest) {
			return;
		}
		String token = tokens.forInstallation(installationId);
		contexts.build(token, repository, number, headSha).ifPresent(context -> {
			log.info("Reviewing {}#{}", repository, number);
			reviewer.review(context);
		});
	}

	/** Tells the classroom what the tests said. Whatever goes wrong here must not stop the review. */
	private void recordProgress(String repository, Integer number, long installationId, String headSha) {
		try {
			if (!events.tracks(repository)) {
				return;
			}
			String token = tokens.forInstallation(installationId);
			failedTests.report(token, repository, headSha)
				.ifPresentOrElse(report -> events.testsRan(repository, number, report),
						() -> events.activity(repository));
		}
		catch (RuntimeException e) {
			log.warn("Could not read the test report of {} for the classroom ({})", repository,
					e.getClass().getSimpleName());
		}
	}

	private void issueComment(JsonNode payload) {
		JsonNode comment = payload.path("comment");
		String text = comment.path("body").asString("").strip();
		boolean byPerson = !"Bot".equals(comment.path("user").path("type").asString(""));
		boolean onPullRequest = payload.path("issue").has("pull_request");
		if (!"created".equals(payload.path("action").asString("")) || !byPerson || !onPullRequest
				|| !isHintCommand(text)) {
			return;
		}
		String repository = payload.path("repository").path("full_name").asString("");
		int number = payload.path("issue").path("number").asInt();
		String token = tokens.forInstallation(payload.path("installation").path("id").asLong());
		events.activity(repository);
		GitHubPullRequest pullRequest = github.pullRequest(token, repository, number);
		String request = text.substring(HINT_COMMAND.length()).strip();
		contexts.build(token, repository, number, pullRequest.headSha()).ifPresent(context -> {
			log.info("Hint requested on {}#{}", repository, number);
			hints.respond(context, request);
		});
	}

	private static boolean isHintCommand(String text) {
		String lower = text.toLowerCase(Locale.ROOT);
		return lower.equals(HINT_COMMAND) || lower.startsWith(HINT_COMMAND + " ")
				|| lower.startsWith(HINT_COMMAND + "\n");
	}

}
