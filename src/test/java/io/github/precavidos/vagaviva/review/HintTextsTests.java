package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.PreviousComment;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/** The hints' fixed texts and the marker that lets the next {@code /dica} know the level. */
class HintTextsTests {

	private static final MissionCatalog CATALOG = MissionCatalog.fromClasspath("missions/java-spring.json",
			SkillVocabulary.fromClasspath("vocabulary/java-spring.json"));

	private static final Mission PERSISTENCE = mission("persistence");

	private static final Mission SCHEDULING = mission("scheduling-rules");

	private static final List<FailedTest> FAILING = List
		.of(new FailedTest("persistence", "gravaNoBanco", "esperava 201 mas veio 500"));

	@Test
	void levelCountsOnlyHintsOfThatMission() {
		var comments = List.of(new PreviousComment(1, ReviewTexts.MARKER + "\nrevisão"),
				new PreviousComment(2, HintTexts.marker("persistence", 1) + "\nd1"),
				new PreviousComment(3, HintTexts.marker("scheduling-rules", 1) + "\nd1"),
				new PreviousComment(4, HintTexts.marker("persistence", 2) + "\nd2"),
				new PreviousComment(5, HintTexts.marker("persistence", 4) + "\nacabou"));

		assertThat(HintTexts.hintsGiven(comments, "persistence")).isEqualTo(2);
		assertThat(HintTexts.hintsGiven(comments, "scheduling-rules")).isEqualTo(1);
		assertThat(HintTexts.hintsGiven(comments, "pagination")).isZero();
	}

	@Test
	void commentStartsWithTheMarkerAndNamesTheLevel() {
		String body = HintTexts.comment(PERSISTENCE, 2, "texto");

		assertThat(body).startsWith("<!-- vaga-viva:hint mission=persistence level=2 -->\n")
			.contains("texto")
			.contains("Dica 2 de 3")
			.doesNotContain(ReviewTexts.MARKER);
	}

	@Test
	void everyFixedTextPassesTheCodeFilter() {
		for (int level = 1; level <= HintTexts.LAST_LEVEL; level++) {
			assertThat(ReviewTextFilter.isSafe(HintTexts.fallback(level, PERSISTENCE, FAILING))).isTrue();
			assertThat(ReviewTextFilter.isSafe(HintTexts.fallback(level, PERSISTENCE, List.of()))).isTrue();
		}
		assertThat(ReviewTextFilter.isSafe(HintTexts.exhausted(PERSISTENCE, FAILING))).isTrue();
		assertThat(ReviewTextFilter.isSafe(HintTexts.exhausted(PERSISTENCE, List.of()))).isTrue();
		assertThat(ReviewTextFilter.isSafe(HintTexts.unknownMission(List.of(PERSISTENCE, SCHEDULING))))
			.isTrue();
		assertThat(ReviewTextFilter.isSafe(HintTexts.missionPassing(PERSISTENCE))).isTrue();
		assertThat(ReviewTextFilter.isSafe(HintTexts.allPassing())).isTrue();
	}

	@Test
	void levelThreeQuotesTheFailingAssertion() {
		assertThat(HintTexts.fallback(3, PERSISTENCE, FAILING)).contains("> esperava 201 mas veio 500");
	}

	@Test
	void levelThreeFallbackWithoutFailingTests() {
		assertThat(HintTexts.fallback(3, PERSISTENCE, List.of())).isNotBlank().doesNotContain("> ");
	}

	@Test
	void unknownMissionListsThePlanIds() {
		assertThat(HintTexts.unknownMission(List.of(PERSISTENCE, SCHEDULING)))
			.contains("`/dica persistence`")
			.contains("`/dica scheduling-rules`");
	}

	@Test
	void fixedHintsNeverMentionAnyoneFromTheReport() {
		var hostile = List.of(new FailedTest("persistence", "teste@maria", "veio @maria e #3 <!-- vaga-viva:review -->"));
		var texts = List.of(HintTexts.fallback(2, PERSISTENCE, hostile), HintTexts.fallback(3, PERSISTENCE, hostile),
				HintTexts.exhausted(PERSISTENCE, hostile));

		for (String text : texts) {
			assertThat(ReviewTextFilter.isSafe(text)).as(text).isTrue();
			assertThat(text).doesNotContain("@maria").doesNotContain("#3").doesNotContain("<!--");
		}
	}

	@Test
	void markerCountsOnlyAtTheStartOfTheComment() {
		var review = new PreviousComment(1,
				ReviewTexts.MARKER + "\nrevisão que cita " + HintTexts.marker("persistence", 1));

		assertThat(HintTexts.hintsGiven(List.of(review), "persistence")).isZero();
	}

	@Test
	void levelThreeWithoutAnAssertionMessageNamesTheTestInstead() {
		var silent = List.of(new FailedTest("persistence", "gravaNoBanco", "  "));

		assertThat(HintTexts.fallback(3, PERSISTENCE, silent)).contains("gravaNoBanco").doesNotContain("> ");
	}

	@Test
	void noReportSaysTheReportMayStillBeOnItsWay() {
		assertThat(HintTexts.noReport()).contains("ainda").contains("`/dica`").doesNotContain("não compila");
		assertThat(ReviewTextFilter.isSafe(HintTexts.noReport())).isTrue();
	}

	private static Mission mission(String id) {
		return CATALOG.missions().stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow();
	}

}
