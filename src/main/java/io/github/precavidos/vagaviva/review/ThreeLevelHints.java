package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;

/**
 * Answers {@code /dica} with the next hint about one mission: level 1
 * (direction), 2 (approach), 3 (almost there), then that the hints are over.
 * The mission is the one the student names, or the first one of the plan with
 * failing tests (ADR 0002: the report says which, not the LLM). The level is
 * counted from the app's own earlier hint comments on the pull request, so
 * nothing is stored; each hint is a new comment for the same reason.
 * <p>
 * Without a store, two {@code /dica} comments handled at the same time read
 * the same earlier comments and may both post the same level. That is the
 * price of ADR 0003; the next {@code /dica} counts both and moves on.
 */
@Component
class ThreeLevelHints implements HintResponder {

	private static final Logger log = LoggerFactory.getLogger(ThreeLevelHints.class);

	private final GitHubApi github;

	private final HintWriter writer;

	private final ReviewEvents events;

	ThreeLevelHints(GitHubApi github, HintWriter writer, ReviewEvents events) {
		this.github = github;
		this.writer = writer;
		this.events = events;
	}

	@Override
	public void respond(ReviewContext context, String request) {
		String requested = request.isBlank() ? "" : request.strip().split("\\s+", 2)[0];
		Mission mission;
		if (!requested.isEmpty()) {
			Optional<Mission> named = context.plan()
				.missions()
				.stream()
				.filter(m -> m.id().toLowerCase(Locale.ROOT).equals(requested.toLowerCase(Locale.ROOT)))
				.findFirst();
			if (named.isEmpty()) {
				post(context, HintTexts.unknownMission(context.plan().missions()));
				return;
			}
			mission = named.get();
			if (context.testReportRead() && failing(context, mission).isEmpty()) {
				post(context, HintTexts.missionPassing(mission));
				return;
			}
		}
		else if (!context.testReportRead()) {
			post(context, HintTexts.noReport());
			return;
		}
		else if (context.failedTests().isEmpty()) {
			post(context, HintTexts.allPassing());
			return;
		}
		else {
			String focusId = context.failedTests().getFirst().missionId();
			mission = context.plan().missions().stream().filter(m -> m.id().equals(focusId)).findFirst().orElseThrow();
		}
		hint(context, mission);
	}

	private void hint(ReviewContext context, Mission mission) {
		List<FailedTest> failing = failing(context, mission);
		int level = HintTexts.hintsGiven(context.reviewerComments(), mission.id()) + 1;
		if (level > HintTexts.LAST_LEVEL) {
			post(context, HintTexts.comment(mission, HintTexts.LAST_LEVEL + 1, HintTexts.exhausted(mission, failing)));
			return;
		}
		List<String> earlier = context.reviewerComments()
			.stream()
			.filter(comment -> HintTexts.isHintAbout(comment, mission.id()))
			.map(HintTexts::text)
			.toList();
		Optional<String> written = writer.write(context, mission, level, failing,
				MissionContract.read(github, context, mission), earlier);
		if (written.isPresent() && !ReviewTextFilter.isSafe(written.get())) {
			log.warn("Hint text refused by the filter for {}#{}", context.repositoryFullName(),
					context.pullRequestNumber());
			written = Optional.empty();
		}
		String text = written.orElseGet(() -> HintTexts.fallback(level, mission, failing));
		post(context, HintTexts.comment(mission, level, text));
		log.info("Hint {} about {} posted on {}#{}", level, mission.id(), context.repositoryFullName(),
				context.pullRequestNumber());
		events.hintGiven(context.repositoryFullName(), mission.id());
	}

	private static List<FailedTest> failing(ReviewContext context, Mission mission) {
		return context.failedTests().stream().filter(test -> test.missionId().equals(mission.id())).toList();
	}

	private void post(ReviewContext context, String body) {
		github.createIssueComment(context.token(), context.repositoryFullName(), context.pullRequestNumber(), body);
	}

}
