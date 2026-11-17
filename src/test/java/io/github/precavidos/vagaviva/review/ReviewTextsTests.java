package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/** The reviewer's fixed texts quote only what is safe to say in the app's name. */
class ReviewTextsTests {

	private static final MissionCatalog CATALOG = MissionCatalog.fromClasspath("missions/java-spring.json",
			SkillVocabulary.fromClasspath("vocabulary/java-spring.json"));

	private static final Mission BUG_HUNT = CATALOG.missions()
		.stream()
		.filter(m -> m.id().equals("bug-hunt"))
		.findFirst()
		.orElseThrow();

	/** The assertion message of case 10 of the reviewer test battery (docs/bateria-revisor.md). */
	static final String CODE_IN_MESSAGE = "olha\n```java\nreturn Duration.between(start, end).toMinutes();\n```";

	@Test
	void fallbackNeverRendersStudentCodeBlock() {
		var test = new FailedTest("bug-hunt", "endsOnTheNextDay", CODE_IN_MESSAGE);

		String text = ReviewTexts.fallback(BUG_HUNT, List.of(test));

		assertThat(text).doesNotContain("```").doesNotContain("return Duration").contains("endsOnTheNextDay");
		assertThat(ReviewTextFilter.isSafe(text)).as(text).isTrue();
	}

	@Test
	void fallbackNeverQuotesASingleLineOfCode() {
		var test = new FailedTest("bug-hunt", "endsOnTheNextDay", "return (int) Duration.between(start, end).toMinutes();");

		String text = ReviewTexts.fallback(BUG_HUNT, List.of(test));

		assertThat(text).doesNotContain("Duration.between");
		assertThat(ReviewTextFilter.isSafe(text)).as(text).isTrue();
	}

	@Test
	void fallbackStillQuotesAPlainAssertionMessage() {
		var test = new FailedTest("bug-hunt", "endsOnTheNextDay", "esperava 60 minutos entre 23:30 e 00:30 do dia seguinte");

		assertThat(ReviewTexts.fallback(BUG_HUNT, List.of(test))).contains("> esperava 60 minutos");
	}

}
