package io.github.precavidos.vagaviva.watch;

/**
 * A posting found on a company's career page.
 *
 * @param url the posting's normalized address
 * @param title the title the page shows for it, blank when it shows none
 */
record ListedPosting(String url, String title) {
}
