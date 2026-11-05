package io.github.precavidos.vagaviva.vocabulary;

/** An ambiguous term that no vocabulary entry may use. */
public record RejectedTerm(String term, String reason) {
}
