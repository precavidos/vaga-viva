package io.github.precavidos.vagaviva.writing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.anthropic.client.okhttp.AnthropicOkHttpClient;

import io.github.precavidos.vagaviva.jobposting.ExtractedJobPosting;
import io.github.precavidos.vagaviva.jobposting.JobPostingExtractor;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.plan.ProjectPlan;
import io.github.precavidos.vagaviva.plan.ProjectPlanner;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/**
 * Measures what the student texts cost with the real Anthropic API. It only
 * runs when both {@code -Dvagaviva.measure-llm-cost=true} and
 * {@code ANTHROPIC_API_KEY} are set, so normal builds and CI skip it.
 */
@EnabledIfSystemProperty(named = "vagaviva.measure-llm-cost", matches = "true")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class LlmCostMeasurement {

	/** Haiku input price in US$ per million tokens, as of 2026-11-06. */
	private static final double INPUT_USD_PER_MTOK = 0.10;

	/** Haiku output price in US$ per million tokens, as of 2026-11-06. */
	private static final double OUTPUT_USD_PER_MTOK = 0.50;

	private static final String MODEL = "claude-haiku-5-5";

	private static final double BUDGET_USD = 5.0;

	private static final String[] POSTINGS = { "estagio.txt", "junior.txt", "pleno.txt" };

	@Test
	void measureCostOfThreePostings() {
		SkillVocabulary vocabulary = SkillVocabulary.fromClasspath("vocabulary/java-spring.json");
		var extractor = new JobPostingExtractor(vocabulary);
		var planner = new ProjectPlanner(MissionCatalog.fromClasspath("missions/java-spring.json", vocabulary));
		var writer = new ClaudeMissionTextWriter(AnthropicOkHttpClient.fromEnv(), MODEL, vocabulary);

		double total = 0;
		System.out.printf(Locale.ROOT, "arquivo | missões | inputTokens | outputTokens | custo US$%n");
		for (String name : POSTINGS) {
			ExtractedJobPosting posting = extractor.extract(read(name));
			ProjectPlan plan = planner.plan(posting);
			WrittenTexts texts = writer.write(plan);
			assertThat(texts.usage()).isPresent();

			TokenUsage usage = texts.usage().get();
			double cost = usage.inputTokens() * INPUT_USD_PER_MTOK / 1e6
					+ usage.outputTokens() * OUTPUT_USD_PER_MTOK / 1e6;
			total += cost;
			String missions = plan.missions().stream().map(m -> m.mission().id()).collect(Collectors.joining(","));
			System.out.printf(Locale.ROOT, "%s | %s | %d | %d | %.6f%n", name, missions, usage.inputTokens(),
					usage.outputTokens(), cost);
			printTexts(texts);
		}

		double average = total / POSTINGS.length;
		System.out.printf(Locale.ROOT, "média por aluno: US$ %.6f%n", average);
		System.out.printf(Locale.ROOT, "alunos que cabem em US$ %.2f: %d%n", BUDGET_USD,
				(long) Math.floor(BUDGET_USD / average));
	}

	private static void printTexts(WrittenTexts texts) {
		System.out.printf(Locale.ROOT, "projectIntro: %s%n", texts.projectIntro().orElse("(padrão)"));
		for (Map.Entry<String, String> entry : texts.missionContexts().entrySet()) {
			System.out.printf(Locale.ROOT, "%s: %s%n", entry.getKey(), entry.getValue());
		}
	}

	private static String read(String name) {
		try (InputStream input = LlmCostMeasurement.class.getResourceAsStream("/llm-cost/" + name)) {
			assertThat(input).as("posting %s on the classpath", name).isNotNull();
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
