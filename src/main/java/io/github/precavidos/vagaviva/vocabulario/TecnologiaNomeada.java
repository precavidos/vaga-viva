package io.github.precavidos.vagaviva.vocabulario;

import java.util.List;

/**
 * Tecnologia reconhecida numa vaga só pelo próprio nome ou pelos termos
 * listados, ligada às competências que ela evidencia.
 */
public record TecnologiaNomeada(String id, String nome, List<String> termos, List<String> competencias) {

	public TecnologiaNomeada {
		termos = termos == null ? List.of() : List.copyOf(termos);
		competencias = competencias == null ? List.of() : List.copyOf(competencias);
	}

}
