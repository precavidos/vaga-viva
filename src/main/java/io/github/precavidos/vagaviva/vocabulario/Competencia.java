package io.github.precavidos.vagaviva.vocabulario;

import java.util.List;

/**
 * Habilidade do vocabulário fechado de um arquétipo. Vem da ESCO quando tem
 * {@code esco}; caso contrário, é da lista própria do projeto.
 */
public record Competencia(String id, String rotulo, Esco esco, List<String> termos) {

	public enum Origem {
		ESCO, PROPRIA
	}

	public Competencia {
		termos = termos == null ? List.of() : List.copyOf(termos);
	}

	public Origem origem() {
		return esco == null ? Origem.PROPRIA : Origem.ESCO;
	}

	/** Referência a uma competência da ESCO, com os rótulos oficiais. */
	public record Esco(String uri, String rotuloPt, String rotuloEn) {

		static final String PREFIXO_URI = "http://data.europa.eu/esco/skill/";

		boolean uriValida() {
			return uri != null && uri.startsWith(PREFIXO_URI) && uri.length() > PREFIXO_URI.length();
		}

	}

}
