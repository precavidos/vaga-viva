package io.github.precavidos.vagaviva.writing;

/** Tokens a model call consumed, for logging and cost measurement. */
public record TokenUsage(long inputTokens, long outputTokens) {
}
