package io.github.precavidos.vagaviva.jobposting;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SeniorityDetectorTests {

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			Vaga de Estágio em Desenvolvimento         | INTERN
			Procuramos estagiária de backend           | INTERN
			Software Engineering Internship            | INTERN
			Desenvolvedor Java Júnior                  | JUNIOR
			Junior Backend Developer                   | JUNIOR
			Dev Jr. Java                               | JUNIOR
			Desenvolvedor Java Pleno                   | MID
			Engenheira de Software Pleno               | MID
			Nível: pleno                               | MID
			Mid-level Java Developer                   | MID
			Desenvolvedor Sênior                       | SENIOR
			Senior Software Engineer                   | SENIOR
			""")
	void detectsASingleLevel(String text, SeniorityLevel expected) {
		assertThat(SeniorityDetector.detect(text)).containsExactly(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Exigimos pleno domínio de inglês", "Sistema em pleno funcionamento",
			"O Sr. João fará a entrevista", "Desenvolvedor Java", "" })
	void doesNotInventALevel(String text) {
		assertThat(SeniorityDetector.detect(text)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "Desenvolvedor Júnior/Pleno", "Vaga júnior ou pleno", "Dev Pleno/Júnior" })
	void detectsARangeOfLevelsInOrder(String text) {
		assertThat(SeniorityDetector.detect(text)).containsExactly(SeniorityLevel.JUNIOR, SeniorityLevel.MID);
	}

}
