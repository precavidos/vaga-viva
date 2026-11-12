package io.github.precavidos.vagaviva.review;

import java.util.List;
import java.util.stream.Collectors;

import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.PreviousComment;

/**
 * The reviewer's fixed texts, in Portuguese: for when there is nothing for
 * the LLM to explain, and the fallback whenever the LLM is off, fails or
 * writes something the filter refuses. They name what the tests expect and
 * ask; they never show code.
 */
final class ReviewTexts {

	/** Hidden marker that tells the reviewer's own comment apart from the hints' and the student's. */
	static final String MARKER = "<!-- vaga-viva:review -->";

	static final String FOOTER = "\n\n---\n<sub>Revisor do Vaga Viva. Travou? Comente `/dica` neste PR para receber uma dica, uma de cada vez.</sub>";

	private ReviewTexts() {
	}

	/** Whether {@code comment} is the reviewer's: its marker opens the body, as {@link #comment} writes it. */
	static boolean isReview(PreviousComment comment) {
		return comment.body().startsWith(MARKER);
	}

	static String comment(String text) {
		return MARKER + "\n" + text.strip() + FOOTER;
	}

	/** The workflow finished without a test report: most often the code does not compile. */
	static String noReport() {
		return """
				O workflow **Vaga Viva** terminou sem gerar o relatório de testes, então os testes nem chegaram a rodar.

				Na maioria das vezes, isso quer dizer que o projeto não compila. Abra a aba **Actions** deste PR, entre no passo **Testes** e procure a primeira mensagem de erro: em que arquivo e linha ela aponta? Depois de corrigir, faça um novo push e eu reviso de novo.""";
	}

	static String allPassing(List<Mission> plan) {
		return "Todos os testes das missões do seu plano passam neste PR: "
				+ plan.stream().map(Mission::title).collect(Collectors.joining(", ")) + ".\n\n"
				+ "Antes de fazer o merge, releia o seu código: tem algum nome que poderia explicar melhor o que faz? Algum trecho repetido que caberia num lugar só?";
	}

	/** The fixed question built from the first failing test of the mission in focus. */
	static String fallback(Mission mission, List<FailedTest> failing) {
		FailedTest first = failing.getFirst();
		String count = failing.size() == 1 ? "1 teste ainda falha" : failing.size() + " testes ainda falham";
		String says = first.message().isBlank()
				? "O primeiro, **" + ReviewTextFilter.neutralize(first.testName()) + "**, falha sem mensagem de asserção.\n\n"
				: "O primeiro diz:\n\n> " + ReviewTextFilter.neutralize(first.message()) + "\n\n";
		return "Na missão **" + mission.title() + "**, " + count + ". " + says
				+ "O que esse teste espera receber, e o que a sua implementação devolve hoje nesse caso? "
				+ "Releia o contrato da missão em `missoes/" + mission.id() + ".md` e compare com o que o teste pede.";
	}

}
