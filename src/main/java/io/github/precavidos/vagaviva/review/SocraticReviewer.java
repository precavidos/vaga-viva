package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubFile;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.PreviousComment;

/**
 * Comments on a student's pull request after the Vaga Viva workflow ran: which
 * mission still fails and questions that lead to the fix, never the fix. Which
 * tests fail comes from the report, not from the LLM (ADR 0002). One comment
 * per pull request, edited on each run instead of piling up.
 */
@Component
class SocraticReviewer implements PullRequestReviewer {

	private static final Logger log = LoggerFactory.getLogger(SocraticReviewer.class);

	private final GitHubApi github;

	private final ReviewWriter writer;

	private final ReviewEvents events;

	SocraticReviewer(GitHubApi github, ReviewWriter writer, ReviewEvents events) {
		this.github = github;
		this.writer = writer;
		this.events = events;
	}

	@Override
	public void review(ReviewContext context) {
		String text;
		if (!context.testReportRead()) {
			text = ReviewTexts.noReport();
		}
		else if (context.failedTests().isEmpty()) {
			text = ReviewTexts.allPassing(context.plan().missions());
		}
		else {
			text = aboutFirstFailingMission(context);
		}
		post(context, ReviewTexts.comment(text));
		events.reviewerAsked(context.repositoryFullName(), context.pullRequestNumber(), text);
	}

	private String aboutFirstFailingMission(ReviewContext context) {
		String focusId = context.failedTests().getFirst().missionId();
		Mission focus = context.plan().missions().stream().filter(m -> m.id().equals(focusId)).findFirst().orElseThrow();
		List<FailedTest> failing = context.failedTests().stream().filter(t -> t.missionId().equals(focusId)).toList();
		String contract = github.readFile(context.token(), context.repositoryFullName(), "missoes/" + focusId + ".md")
			.map(GitHubFile::content)
			.orElse(focus.summary());
		Optional<String> written = writer.write(context, focus, failing, contract);
		if (written.isPresent() && !ReviewTextFilter.isSafe(written.get())) {
			log.warn("Review text refused by the filter for {}#{}", context.repositoryFullName(),
					context.pullRequestNumber());
			written = Optional.empty();
		}
		return written.orElseGet(() -> ReviewTexts.fallback(focus, failing));
	}

	private void post(ReviewContext context, String body) {
		Optional<PreviousComment> previous = context.reviewerComments()
			.stream()
			.filter(comment -> comment.body().contains(ReviewTexts.MARKER))
			.reduce((first, second) -> second);
		if (previous.isPresent()) {
			github.updateIssueComment(context.token(), context.repositoryFullName(), previous.get().id(), body);
		}
		else {
			github.createIssueComment(context.token(), context.repositoryFullName(), context.pullRequestNumber(), body);
		}
	}

}
