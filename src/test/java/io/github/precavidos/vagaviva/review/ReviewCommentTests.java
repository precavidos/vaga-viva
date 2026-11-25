package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.review.ReviewComment.Summary;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/** The review comment quotes only what is safe to say in the app's name, and reads back as plain text. */
class ReviewCommentTests {

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
	void neverRendersStudentCodeBlock() {
		String body = comment(new FailedTest("bug-hunt", "endsOnTheNextDay", CODE_IN_MESSAGE));

		assertThat(body).doesNotContain("```").doesNotContain("return Duration").contains("`endsOnTheNextDay`");
	}

	@Test
	void neverQuotesASingleLineOfCode() {
		String body = comment(
				new FailedTest("bug-hunt", "endsOnTheNextDay", "return (int) Duration.between(start, end).toMinutes();"));

		assertThat(body).doesNotContain("Duration.between");
	}

	@Test
	void stillQuotesAPlainAssertionMessage() {
		assertThat(comment(new FailedTest("bug-hunt", "endsOnTheNextDay",
				"esperava 60 minutos entre 23:30 e 00:30 do dia seguinte")))
			.contains("| `endsOnTheNextDay` | esperava 60 minutos entre 23:30 e 00:30 do dia seguinte |");
	}

	@Test
	void dropsTheResponseBodyAndCutsLongMessages() {
		assertThat(ReviewComment.shortened("esperava status 404 ao alterar um paciente removido; PUT /patients/{id} "
				+ "respondeu 200 com corpo: {\"id\":1,\"email\":\"paciente8@example.com\"}"))
			.isEqualTo("esperava status 404 ao alterar um paciente removido; PUT /patients/{id} respondeu 200");
		assertThat(ReviewComment.shortened("expected 201 but got 500 with body: {\"error\":\"boom\"}"))
			.isEqualTo("expected 201 but got 500");

		String cut = ReviewComment.shortened("esperava " + "a resposta certa ".repeat(20));
		assertThat(cut).hasSizeLessThanOrEqualTo(ReviewComment.MAX_MESSAGE + 1).endsWith("…");
		assertThat(ReviewComment.shortened("esperava 60 minutos")).isEqualTo("esperava 60 minutos");
	}

	@Test
	void theTableShowsTheShortenedMessage() {
		assertThat(comment(new FailedTest("bug-hunt", "deletesPatient",
				"esperava status 404 ao buscar um paciente removido; GET /patients/{id} respondeu 200 com corpo: {\"id\":1}")))
			.contains("| `deletesPatient` | esperava status 404 ao buscar um paciente removido; GET /patients/{id} respondeu 200 |");
	}

	@Test
	void fallbackQuestionsPassTheFilter() {
		List<String> questions = ReviewComment.fallbackQuestions(BUG_HUNT,
				List.of(new FailedTest("bug-hunt", "endsOnTheNextDay", CODE_IN_MESSAGE)));

		assertThat(questions).allSatisfy(q -> assertThat(ReviewTextFilter.isSafe(q)).as(q).isTrue());
	}

	@Test
	void stripsTheNumberingTheModelAdds() {
		assertThat(ReviewComment.cleanQuestions(List.of("1. Primeira?", "Pergunta 2: Segunda?", "3) Terceira?", "Quarta?")))
			.containsExactly("Primeira?", "Segunda?", "Terceira?");
	}

	@Test
	void readsBackOlderCommentsWithoutTheSummary() {
		assertThat(ReviewTexts.reviewText(ReviewTexts.MARKER + "\n  Uma pergunta antiga  " + ReviewTexts.FOOTER))
			.contains("Uma pergunta antiga");
		assertThat(ReviewTexts.reviewText("<!-- vaga-viva:hint mission=x level=1 -->\nDica")).isEmpty();
	}

	@Test
	void summaryLosesTheMarkdownEmphasis() {
		assertThat(new Summary("Na missão **X**, 1 de 3 testes já passam.", List.of("O que o teste **a** espera?"))
			.plainText()).isEqualTo("Na missão X, 1 de 3 testes já passam.\n\n1. O que o teste a espera?");
	}

	private static String comment(FailedTest test) {
		var plan = new StudentPlan("java-spring", "clinic-scheduling", List.of(BUG_HUNT));
		var context = new ReviewContext("token", "maria/repo", 7, "abc", plan, "diff", true, List.of(test), List.of(),
				Map.of("bug-hunt", new MissionTally(0, 4)));
		var summary = new Summary(ReviewComment.fallbackWorking(context, BUG_HUNT),
				ReviewComment.fallbackQuestions(BUG_HUNT, List.of(test)));
		return ReviewComment.focus(context, BUG_HUNT, List.of(test), summary, "./mvnw test -Dgroups=bug-hunt",
				Optional.empty());
	}

}
