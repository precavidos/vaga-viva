package io.github.precavidos.vagaviva.vocabulario;

/** Termo ambíguo que nenhuma entrada do vocabulário pode usar. */
public record TermoRecusado(String termo, String motivo) {
}
