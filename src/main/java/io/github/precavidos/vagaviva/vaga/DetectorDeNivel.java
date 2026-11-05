package io.github.precavidos.vagaviva.vaga;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.precavidos.vagaviva.vocabulario.Normalizacao;

/**
 * Detecta a senioridade pedida numa vaga. Como no vocabulário, prefere não
 * reconhecer a inventar: "pleno" só conta perto de um cargo ou de outro nível
 * ("pleno domínio de inglês" não é nível), e "Sr." fica de fora porque também
 * é "Senhor".
 */
final class DetectorDeNivel {

	private static final String INICIO = "(?<![\\p{L}\\p{N}])";

	private static final String FIM = "(?![\\p{L}\\p{N}])";

	private static final String CARGO = "(?:desenvolvedora?|dev|programadora?|engenheir[oa]|analista|nivel)";

	private static final String SEPARADOR = "[\\s:,\\-]+";

	private static final Map<Nivel, Pattern> PADROES = Map.of(
			Nivel.ESTAGIO, padrao("(?:estagio|estagiari[oa]|intern|internship)"),
			Nivel.JUNIOR, padrao("(?:junior|jr)"),
			Nivel.PLENO, padrao("(?:mid[\\s-]level"
					+ "|" + CARGO + "(?:" + SEPARADOR + "[\\p{L}\\p{N}]+){0,2}" + SEPARADOR + "pleno"
					+ "|(?:estagio|junior|senior)\\s*(?:/|ou|e)\\s*pleno"
					+ "|pleno\\s*(?:/|ou|e)\\s*(?:junior|senior))"),
			Nivel.SENIOR, padrao("senior"));

	private DetectorDeNivel() {
	}

	/** Os níveis citados na vaga, do menor para o maior; vazio se nenhum foi reconhecido. */
	static List<Nivel> detectar(String texto) {
		String normalizado = Normalizacao.normalizar(texto);
		var niveis = EnumSet.noneOf(Nivel.class);
		PADROES.forEach((nivel, padrao) -> {
			if (padrao.matcher(normalizado).find()) {
				niveis.add(nivel);
			}
		});
		return List.copyOf(niveis);
	}

	private static Pattern padrao(String expressao) {
		return Pattern.compile(INICIO + expressao + FIM);
	}

}
