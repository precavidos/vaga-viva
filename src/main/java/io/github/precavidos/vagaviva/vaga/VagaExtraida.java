package io.github.precavidos.vagaviva.vaga;

import java.util.List;

import io.github.precavidos.vagaviva.vocabulario.Reconhecimento;

/** O que sobra de uma vaga depois da leitura: o texto em si não faz parte. */
public record VagaExtraida(String arquetipo, List<Nivel> niveis, Reconhecimento reconhecimento) {

	public VagaExtraida {
		niveis = List.copyOf(niveis);
	}

}
