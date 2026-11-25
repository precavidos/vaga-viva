package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.precavidos.vagaviva.archetype.ArchetypeCatalog;
import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewComment.Summary;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.PreviousComment;

/**
 * Comments on a student's pull request after the Vaga Viva workflow ran: the progress of the plan, which mission
 * still fails and questions that lead to the fix, never the fix. Which tests fail and how many pass come from the
 * report, not from the LLM (ADR 0002); the LLM only writes what already works and the questions, and each part goes
 * through {@link ReviewTextFilter} on its own. One comment per pull request, edited on each run.
 */
@Component
class SocraticReviewer implements PullRequestReviewer {

	private static final Logger log = LoggerFactory.getLogger(SocraticReviewer.class);

	private final GitHubApi github;

	private final ReviewWriter writer;

	private final ReviewEvents events;

	private final ArchetypeCatalog archetypes;

	SocraticReviewer(GitHubApi github, ReviewWriter writer, ReviewEvents events, ArchetypeCatalog archetypes) {
		this.github = github;
		this.writer = writer;
		this.events = events;
		this.archetypes = archetypes;
	}

	@Override
	public void review(ReviewContext context) {
		String body;
		String asked;
		if (!context.testReportRead()) {
			body = ReviewComment.noReport();
			asked = "Os testes não rodaram neste PR. Qual é a primeira mensagem de erro no passo Testes da aba Actions?";
		}
		else if (context.failedTests().isEmpty()) {
			body = ReviewComment.allPassing(context);
			asked = "Todas as missões do plano passam neste PR.";
		}
		else {
			String focusId = context.failedTests().getFirst().missionId();
			Mission focus = mission(context, focusId);
			List<FailedTest> failing = context.failedTests().stream().filter(t -> t.missionId().equals(focusId)).toList();
			Summary summary = summary(context, focus, failing);
			body = ReviewComment.focus(context, focus, failing, summary, testCommand(context, focus),
					justCompleted(context, focus));
			asked = summary.questions().getFirst();
		}
		post(context, body);
		events.reviewerAsked(context.repositoryFullName(), context.pullRequestNumber(), asked);
	}

	/** The LLM's parts, each kept only if the filter accepts it; the fixed text fills whatever is missing. */
	private Summary summary(ReviewContext context, Mission focus, List<FailedTest> failing) {
		Optional<ReviewWriter.Written> written = writer.write(context, focus, failing,
				MissionContract.read(github, context, focus));
		String raw = written.map(ReviewWriter.Written::working).orElse("");
		String working = raw.isEmpty() ? "" : ReviewTextFilter.fromLlm(raw).orElse("");
		if (!raw.isEmpty() && working.isEmpty()) {
			log.warn("Review text (working) refused by the filter for {}#{}", context.repositoryFullName(),
					context.pullRequestNumber());
		}
		List<String> questions = ReviewComment
			.cleanQuestions(written.map(ReviewWriter.Written::questions).orElse(List.of()));
		List<String> safe = questions.stream().flatMap(q -> ReviewTextFilter.fromLlm(q).stream()).toList();
		if (safe.size() < questions.size()) {
			log.warn("Review text ({} of {} questions) refused by the filter for {}#{}",
					questions.size() - safe.size(), questions.size(), context.repositoryFullName(),
					context.pullRequestNumber());
		}
		return new Summary(working.isEmpty() ? ReviewComment.fallbackWorking(context, focus) : working,
				safe.isEmpty() ? ReviewComment.fallbackQuestions(focus, failing) : safe);
	}

	/**
	 * The mission the previous review was about, when it is now complete: the student just finished it. Read from
	 * the previous comment, since nothing is stored (ADR 0003).
	 */
	private Optional<Mission> justCompleted(ReviewContext context, Mission focus) {
		return previousReview(context).flatMap(previous -> ReviewComment.focusOf(previous.body()))
			.filter(id -> !id.equals(focus.id()))
			.filter(id -> {
				MissionTally tally = context.tallies().get(id);
				return tally != null && tally.total() > 0 && tally.passed() == tally.total();
			})
			.flatMap(id -> context.plan().missions().stream().filter(m -> m.id().equals(id)).findFirst());
	}

	private String testCommand(ReviewContext context, Mission focus) {
		try {
			return archetypes.archetype(context.plan().archetype()).missionTestCommand(focus.id());
		}
		catch (IllegalArgumentException e) {
			return archetypes.archetypes().getFirst().missionTestCommand(focus.id());
		}
	}

	private static Mission mission(ReviewContext context, String id) {
		return context.plan().missions().stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow();
	}

	private static Optional<PreviousComment> previousReview(ReviewContext context) {
		return context.reviewerComments().stream().filter(ReviewTexts::isReview).reduce((first, second) -> second);
	}

	private void post(ReviewContext context, String body) {
		Optional<PreviousComment> previous = previousReview(context);
		if (previous.isPresent()) {
			github.updateIssueComment(context.token(), context.repositoryFullName(), previous.get().id(), body);
		}
		else {
			github.createIssueComment(context.token(), context.repositoryFullName(), context.pullRequestNumber(), body);
		}
	}

}
