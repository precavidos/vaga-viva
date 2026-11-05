package io.github.precavidos.vagaviva.vocabulario;

import java.text.Normalizer;
import java.util.Locale;

/** Forma comum para comparar textos: sem acentos, minúscula e com espaços simples. */
public final class Normalizacao {

	private Normalizacao() {
	}

	public static String normalizar(String texto) {
		String semAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return semAcentos.toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
	}

}
