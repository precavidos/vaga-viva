package io.github.precavidos.vagaviva.writing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

class LlmConfigTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(LlmConfig.class)
		.withBean(SkillVocabulary.class, () -> SkillVocabulary.fromClasspath("vocabulary/java-spring.json"));

	@Test
	void usesTheDefaultTextsWithoutAKey() {
		contextRunner.run(context -> assertThat(context.getBean(MissionTextWriter.class))
			.isInstanceOf(DefaultMissionTextWriter.class));
	}

	@Test
	void usesClaudeWhenAKeyIsSet() {
		contextRunner.withPropertyValues("vagaviva.llm.api-key=test-key")
			.run(context -> assertThat(context.getBean(MissionTextWriter.class))
				.isInstanceOf(ClaudeMissionTextWriter.class));
	}

}
