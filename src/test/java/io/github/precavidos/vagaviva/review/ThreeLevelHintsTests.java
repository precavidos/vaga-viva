package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubComment;
import io.github.precavidos.vagaviva.github.GitHubFile;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.PreviousComment;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/** What {@code /dica} posts, with a fake review context and a fake GitHub: no network. */
class ThreeLevelHintsTests {

	private static final String REPO = "maria/vaga-viva-clinic-scheduling";

	private static final MissionCatalog CATALOG = MissionCatalog.fromClasspath("missions/java-spring.json",
			SkillVocabulary.fromClasspath("vocabulary/java-spring.json"));

	private static final Mission PERSISTENCE = mission("persistence");

	private static final Mission SCHEDULING = mission("scheduling-rules");

	private static final StudentPlan PLAN = new StudentPlan("java-spring", "clinic-scheduling",
			List.of(PERSISTENCE, SCHEDULING));

	private final GitHubApi github = mock(GitHubApi.class);

	private final List<String> posts = new ArrayList<>();

	private final List<Integer> levels = new ArrayList<>();

	private final List<String> contracts = new ArrayList<>();

	private final List<List<String>> earlierHints = new ArrayList<>();

	private final List<String> hintsGiven = new ArrayList<>();

	private Optional<String> llmText = Optional.of("O que acontece com o paciente quando ele é gravado?");

	private ThreeLevelHints hints;

	@BeforeEach
	void setUp() {
		HintWriter writer = (context, mission, level, failing, contract, earlier) -> {
			levels.add(level);
			contracts.add(contract);
			earlierHints.add(earlier);
			return llmText;
		};
		hints = new ThreeLevelHints(github, writer, new ReviewEvents() {

			@Override
			public boolean tracks(String repositoryFullName) {
				return true;
			}

			@Override
			public void activity(String repositoryFullName) {
			}

			@Override
			public void testsRan(String repositoryFullName, Integer pullRequestNumber, TestReport report) {
			}

			@Override
			public void reviewerAsked(String repositoryFullName, int pullRequestNumber, String question) {
			}

			@Override
			public void hintGiven(String repositoryFullName, String missionId) {
				hintsGiven.add(missionId);
			}

		});
		when(github.readFile("token", REPO, "missoes/persistence.md"))
			.thenReturn(Optional.of(new GitHubFile("# Contrato de persistence", "sha")));
		when(github.createIssueComment(anyString(), anyString(), anyInt(), anyString())).thenAnswer(invocation -> {
			posts.add(invocation.getArgument(3));
			return new GitHubComment(posts.size(), "vaga-viva[bot]", true, invocation.getArgument(3));
		});
	}

	@Test
	void givesLevelOneThenTwoThenThreeThenSaysTheHintsAreOver() {
		var comments = new ArrayList<PreviousComment>();
		for (int i = 0; i < 4; i++) {
			hints.respond(context(true, List.of(failed("persistence")), comments), "");
			comments.add(new PreviousComment(i, posts.getLast()));
		}

		assertThat(levels).containsExactly(1, 2, 3);
		assertThat(posts.get(0)).startsWith(HintTexts.marker("persistence", 1)).contains(llmText.orElseThrow());
		assertThat(posts.get(1)).startsWith(HintTexts.marker("persistence", 2));
		assertThat(posts.get(2)).startsWith(HintTexts.marker("persistence", 3));
		assertThat(posts.get(3)).startsWith(HintTexts.marker("persistence", 4)).contains("acabaram");
		assertThat(hintsGiven).containsExactly("persistence", "persistence", "persistence");
		assertThat(contracts).containsOnly("# Contrato de persistence");
		verify(github, never()).updateIssueComment(anyString(), anyString(), anyLong(), anyString());
	}

	@Test
	void keepsSayingTheHintsAreOverAfterTheLastOne() {
		var comments = List.of(hint("persistence", 1), hint("persistence", 2), hint("persistence", 3),
				hint("persistence", 4));

		hints.respond(context(true, List.of(failed("persistence")), comments), "");

		assertThat(posts.getLast()).startsWith(HintTexts.marker("persistence", 4)).contains("acabaram");
		assertThat(levels).isEmpty();
	}

	@Test
	void defaultsToTheFirstFailingMissionInPlanOrder() {
		hints.respond(context(true, List.of(failed("persistence"), failed("scheduling-rules")), List.of()), "");

		assertThat(posts.getLast()).startsWith(HintTexts.marker("persistence", 1));
	}

	@Test
	void usesTheNamedMissionAndItsOwnLevel() {
		var comments = List.of(hint("persistence", 1), hint("persistence", 2));

		hints.respond(context(true, List.of(failed("persistence"), failed("scheduling-rules")), comments),
				"scheduling-rules");

		assertThat(posts.getLast()).startsWith(HintTexts.marker("scheduling-rules", 1));
		assertThat(hintsGiven).containsExactly("scheduling-rules");
	}

	@Test
	void namedMissionIgnoresCaseAndTrailingWords() {
		hints.respond(context(true, List.of(failed("persistence"), failed("scheduling-rules")), List.of()),
				"Scheduling-Rules obrigado");

		assertThat(posts.getLast()).startsWith(HintTexts.marker("scheduling-rules", 1));
	}

	@Test
	void unknownMissionListsThePlanIds() {
		hints.respond(context(true, List.of(failed("persistence")), List.of()), "xyz");

		assertThat(posts.getLast()).isEqualTo(HintTexts.unknownMission(PLAN.missions())).doesNotContain("xyz");
		assertThat(levels).isEmpty();
		assertThat(hintsGiven).isEmpty();
	}

	@Test
	void missionOutsideThePlanListsThePlanIds() {
		hints.respond(context(true, List.of(failed("persistence")), List.of()), "pagination");

		assertThat(posts.getLast()).isEqualTo(HintTexts.unknownMission(PLAN.missions()));
		assertThat(levels).isEmpty();
	}

	@Test
	void namedMissionThatAlreadyPassesSaysSo() {
		hints.respond(context(true, List.of(failed("persistence")), List.of()), "scheduling-rules");

		assertThat(posts.getLast()).isEqualTo(HintTexts.missionPassing(SCHEDULING));
		assertThat(levels).isEmpty();
		assertThat(hintsGiven).isEmpty();
	}

	@Test
	void namedMissionWithoutAReportStillGetsAHint() {
		hints.respond(context(false, List.of(), List.of()), "persistence");

		assertThat(posts.getLast()).startsWith(HintTexts.marker("persistence", 1));
		assertThat(levels).containsExactly(1);
	}

	@Test
	void noReportAndNoMissionSaysTheTestsDidNotRun() {
		hints.respond(context(false, List.of(), List.of()), "");

		assertThat(posts.getLast()).isEqualTo(HintTexts.noReport());
		assertThat(levels).isEmpty();
		assertThat(hintsGiven).isEmpty();
	}

	@Test
	void everythingPassingHasNoHint() {
		hints.respond(context(true, List.of(), List.of()), "");

		assertThat(posts.getLast()).isEqualTo(HintTexts.allPassing());
		assertThat(levels).isEmpty();
	}

	@Test
	void fallsBackToTheFixedHintWhenTheLlmHasNothing() {
		llmText = Optional.empty();
		var failing = List.of(failed("persistence"));

		hints.respond(context(true, failing, List.of()), "");

		assertThat(posts.getLast()).contains(HintTexts.fallback(1, PERSISTENCE, failing));
		assertThat(hintsGiven).containsExactly("persistence");
	}

	@Test
	void refusesAnLlmHintWithCode() {
		llmText = Optional.of("Use assim:\n```java\nrepository.save(patient);\n```");
		var failing = List.of(failed("persistence"));

		hints.respond(context(true, failing, List.of()), "");

		assertThat(posts.getLast()).doesNotContain("repository.save")
			.contains(HintTexts.fallback(1, PERSISTENCE, failing));
	}

	@Test
	void usesTheMissionSummaryWhenTheContractFileIsMissing() {
		when(github.readFile("token", REPO, "missoes/scheduling-rules.md")).thenReturn(Optional.empty());

		hints.respond(context(true, List.of(failed("scheduling-rules")), List.of()), "");

		assertThat(contracts).containsExactly(SCHEDULING.summary());
	}

	@Test
	void passesOnlyThatMissionsEarlierHintsToTheWriter() {
		var comments = List.of(new PreviousComment(1, ReviewTexts.comment("Revisão do PR")),
				new PreviousComment(2, HintTexts.comment(PERSISTENCE, 1, "Olhe a data")),
				new PreviousComment(3, HintTexts.comment(SCHEDULING, 1, "Olhe o horário")));

		hints.respond(context(true, List.of(failed("persistence")), comments), "");

		assertThat(earlierHints).containsExactly(List.of("Olhe a data"));
		assertThat(levels).containsExactly(2);
	}

	private static ReviewContext context(boolean reportRead, List<FailedTest> failed, List<PreviousComment> comments) {
		return new ReviewContext("token", REPO, 7, "abc123", PLAN, "diff --git a/x b/x", reportRead, failed, comments);
	}

	private static FailedTest failed(String mission) {
		return new FailedTest(mission, "teste_" + mission, "esperava algo em " + mission);
	}

	private static PreviousComment hint(String mission, int level) {
		return new PreviousComment(level, HintTexts.marker(mission, level) + "\ndica");
	}

	private static Mission mission(String id) {
		return CATALOG.missions().stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow();
	}

}
