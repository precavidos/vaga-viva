package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** The last line of defence: nothing that looks like code reaches the student. */
class ReviewTextFilterTests {

	@ParameterizedTest
	@ValueSource(strings = {
			"O teste espera status 201. O que a sua rota devolve hoje quando o paciente é criado?",
			"Você já criou a rota `POST /patients`. Onde o `Location` deveria ser preenchido?",
			"Compare com o que diz `missoes/persistence.md`: qual coluna falta na tabela `patients`?",
			"Duas perguntas:\n- O que acontece com a data quando a consulta passa da meia-noite?\n- Qual teste mostra isso?",
			"Isso vale para C# também, e escreva para contato@example.com se precisar." })
	void letsQuestionsAndShortIdentifiersThrough(String text) {
		assertThat(ReviewTextFilter.isSafe(text)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"Faça assim:\n```java\nrepository.save(patient);\n```",
			"Use ~~~\nreturn ok;\n~~~",
			"Basta escrever `repository.save(patient)` no service.",
			"Troque por `if (start.isBefore(now)) throw new BadRequest()`.",
			"Uma linha resolve:\nreturn ResponseEntity.created(uri).body(patient);",
			"Crie a classe assim:\npublic class PatientController {",
			"Exemplo:\n    patients.put(id, patient)",
			"@PostMapping",
			"## Solução",
			"Como discutido com @maria, veja o #3.",
			"const total = items.reduce((a, b) => a + b);",
			"" })
	void refusesAnythingThatLooksLikeCode(String text) {
		assertThat(ReviewTextFilter.isSafe(text)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(ints = { ReviewTextFilter.MAX_LENGTH + 1 })
	void refusesVeryLongTexts(int length) {
		assertThat(ReviewTextFilter.isSafe("a".repeat(length))).isFalse();
	}

}
