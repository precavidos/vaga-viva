package io.github.precavidos.vagaviva.vaga;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DetectorDeNivelTests {

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			Vaga de Estágio em Desenvolvimento         | ESTAGIO
			Procuramos estagiária de backend           | ESTAGIO
			Software Engineering Internship            | ESTAGIO
			Desenvolvedor Java Júnior                  | JUNIOR
			Junior Backend Developer                   | JUNIOR
			Dev Jr. Java                               | JUNIOR
			Desenvolvedor Java Pleno                   | PLENO
			Engenheira de Software Pleno               | PLENO
			Nível: pleno                               | PLENO
			Mid-level Java Developer                   | PLENO
			Desenvolvedor Sênior                       | SENIOR
			Senior Software Engineer                   | SENIOR
			""")
	void detectaUmNivel(String texto, Nivel esperado) {
		assertThat(DetectorDeNivel.detectar(texto)).containsExactly(esperado);
	}

	@ParameterizedTest
	@ValueSource(strings = { "Exigimos pleno domínio de inglês", "Sistema em pleno funcionamento",
			"O Sr. João fará a entrevista", "Desenvolvedor Java", "" })
	void naoInventaNivel(String texto) {
		assertThat(DetectorDeNivel.detectar(texto)).isEmpty();
	}

	@ParameterizedTest
	@ValueSource(strings = { "Desenvolvedor Júnior/Pleno", "Vaga júnior ou pleno", "Dev Pleno/Júnior" })
	void detectaFaixaDeNiveisEmOrdem(String texto) {
		assertThat(DetectorDeNivel.detectar(texto)).containsExactly(Nivel.JUNIOR, Nivel.PLENO);
	}

}
