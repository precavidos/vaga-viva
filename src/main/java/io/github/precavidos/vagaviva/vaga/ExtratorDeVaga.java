package io.github.precavidos.vagaviva.vaga;

import org.springframework.stereotype.Service;

import io.github.precavidos.vagaviva.vocabulario.VocabularioDeCompetencias;

/**
 * Lê o texto de uma vaga e devolve o que foi reconhecido nela. O texto só
 * existe em memória durante a chamada: não é guardado nem registrado em log
 * (ADR 0003).
 */
@Service
public class ExtratorDeVaga {

	/** Limite de caracteres do texto da vaga; anúncios reais ficam bem abaixo disso. */
	public static final int TAMANHO_MAXIMO = 20_000;

	private final VocabularioDeCompetencias vocabulario;

	ExtratorDeVaga(VocabularioDeCompetencias vocabulario) {
		this.vocabulario = vocabulario;
	}

	public VagaExtraida extrair(String texto) {
		return new VagaExtraida(vocabulario.arquetipo(), DetectorDeNivel.detectar(texto), vocabulario.reconhecer(texto));
	}

}
