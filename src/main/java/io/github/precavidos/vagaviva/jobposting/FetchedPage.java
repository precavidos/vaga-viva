package io.github.precavidos.vagaviva.jobposting;

/**
 * A downloaded career page, kept only in memory (ADR 0003).
 *
 * @param html the response body, as sent by the server
 * @param charset the charset named by the {@code Content-Type} header, or
 * {@code null} when the header names none (or one Java does not support), in
 * which case the page's own {@code <meta charset>} decides
 */
record FetchedPage(byte[] html, String charset) {
}
