package io.github.precavidos.vagaviva.vocabulario;

import java.util.List;

/**
 * O que o vocabulário reconheceu num texto. As listas seguem a ordem do
 * vocabulário, e cada item guarda os termos do texto que o evidenciam.
 */
public record Reconhecimento(List<TecnologiaReconhecida> tecnologias, List<CompetenciaReconhecida> competencias) {

	public Reconhecimento {
		tecnologias = List.copyOf(tecnologias);
		competencias = List.copyOf(competencias);
	}

	public record TecnologiaReconhecida(TecnologiaNomeada tecnologia, List<String> termosEncontrados) {

		public TecnologiaReconhecida {
			termosEncontrados = List.copyOf(termosEncontrados);
		}

	}

	/**
	 * Competência reconhecida pelos próprios termos ou por uma tecnologia que a
	 * evidencia; {@code evidencias} traz os termos que levaram a ela.
	 */
	public record CompetenciaReconhecida(Competencia competencia, List<String> evidencias) {

		public CompetenciaReconhecida {
			evidencias = List.copyOf(evidencias);
		}

	}

}
