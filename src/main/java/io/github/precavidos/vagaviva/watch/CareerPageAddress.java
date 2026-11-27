package io.github.precavidos.vagaviva.watch;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The career page of a company on a known job platform, and how its posting
 * links look. Only these platforms are accepted: their posting links follow a
 * fixed pattern, so a link on the page is a posting or not without reading it.
 *
 * @param platform the job platform
 * @param url the page's address, normalized, so two students who follow the same company share one page
 * @param company the company's part of the address: a subdomain or the first path segment
 * @param site Workday's career site, the path segment after the optional locale; blank elsewhere
 */
record CareerPageAddress(Platform platform, String url, String company, String site) {

	/** Longest address kept; career page and posting addresses are far shorter. */
	static final int MAX_LENGTH = 500;

	enum Platform {

		GUPY, GREENHOUSE, LEVER, ASHBY, WORKABLE, SMARTRECRUITERS, WORKDAY

	}

	private static final Pattern GUPY_HOST = Pattern.compile("([a-z0-9][a-z0-9-]*)\\.gupy\\.io");

	/** Gupy's own subdomains, which are not a company's page. */
	private static final Set<String> GUPY_RESERVED = Set.of("www", "portal", "login", "app", "api", "help",
			"developers", "blog", "vagas");

	private static final Set<String> GREENHOUSE_HOSTS = Set.of("boards.greenhouse.io", "job-boards.greenhouse.io",
			"boards.eu.greenhouse.io", "job-boards.eu.greenhouse.io");

	private static final Set<String> LEVER_HOSTS = Set.of("jobs.lever.co", "jobs.eu.lever.co");

	private static final Set<String> SMARTRECRUITERS_HOSTS = Set.of("careers.smartrecruiters.com",
			"jobs.smartrecruiters.com");

	private static final Pattern WORKDAY_HOST = Pattern.compile("([a-z0-9][a-z0-9-]*)\\.wd\\d+\\.myworkdayjobs\\.com");

	private static final Pattern LOCALE = Pattern.compile("[a-z]{2}-[A-Za-z]{2}");

	private static final Pattern DIGITS = Pattern.compile("\\d{1,20}");

	private static final Pattern UUID = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

	private static final Pattern WORKABLE_CODE = Pattern.compile("[0-9A-Za-z]{4,20}");

	private static final Pattern SMARTRECRUITERS_POSTING = Pattern.compile("\\d{6,20}(-[^/]*)?");

	private static final Pattern SEGMENT = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

	/** The career page at {@code url}, or empty when it is not a company page of a known platform. */
	static Optional<CareerPageAddress> parse(String url) {
		Optional<URI> parsed = uri(url, null);
		if (parsed.isEmpty()) {
			return Optional.empty();
		}
		String host = parsed.get().getHost().toLowerCase(Locale.ROOT);
		List<String> segments = segments(parsed.get());
		Matcher gupy = GUPY_HOST.matcher(host);
		if (gupy.matches()) {
			return GUPY_RESERVED.contains(gupy.group(1)) ? Optional.empty()
					: page(Platform.GUPY, "https://" + host + "/", gupy.group(1), "");
		}
		Matcher workday = WORKDAY_HOST.matcher(host);
		if (workday.matches()) {
			List<String> rest = withoutLocale(segments);
			return rest.isEmpty() ? Optional.empty()
					: page(Platform.WORKDAY, "https://" + host + "/" + rest.get(0), workday.group(1), rest.get(0));
		}
		if (segments.isEmpty()) {
			return Optional.empty();
		}
		String first = segments.get(0);
		if (GREENHOUSE_HOSTS.contains(host) && !first.equalsIgnoreCase("embed")) {
			String company = first.toLowerCase(Locale.ROOT);
			return page(Platform.GREENHOUSE, "https://" + host + "/" + company, company, "");
		}
		if (LEVER_HOSTS.contains(host)) {
			return page(Platform.LEVER, "https://" + host + "/" + first, first, "");
		}
		if (host.equals("jobs.ashbyhq.com")) {
			return page(Platform.ASHBY, "https://" + host + "/" + first, first, "");
		}
		if (host.equals("apply.workable.com") && !first.equals("j")) {
			return page(Platform.WORKABLE, "https://" + host + "/" + first, first, "");
		}
		if (SMARTRECRUITERS_HOSTS.contains(host)) {
			return page(Platform.SMARTRECRUITERS, "https://" + host + "/" + first, first, "");
		}
		return Optional.empty();
	}

	/** Whether {@code url} is a posting on a known platform, as opposed to any other address. */
	static boolean isKnownPosting(String url) {
		return parse(url).flatMap(page -> page.posting(url)).isPresent();
	}

	/**
	 * The normalized address of the posting a link of this page points to, or
	 * empty when the link is anything else: another company, an application
	 * form, a filter of the list.
	 */
	Optional<String> posting(String link) {
		Optional<URI> parsed = uri(link, URI.create(url));
		if (parsed.isEmpty()) {
			return Optional.empty();
		}
		String host = parsed.get().getHost().toLowerCase(Locale.ROOT);
		List<String> segments = segments(parsed.get());
		Optional<String> posting = switch (platform) {
			case GUPY -> host.equals(host()) && segments.size() == 2 && segments.get(0).equals("jobs")
					&& DIGITS.matcher(segments.get(1)).matches()
							? Optional.of("https://" + host + "/jobs/" + segments.get(1)) : Optional.empty();
			case GREENHOUSE -> GREENHOUSE_HOSTS.contains(host) && segments.size() == 3 && ours(segments.get(0))
					&& segments.get(1).equals("jobs") && DIGITS.matcher(segments.get(2)).matches()
							? Optional.of("https://" + host + "/" + company + "/jobs/" + segments.get(2))
							: Optional.empty();
			case LEVER, ASHBY -> host.equals(host()) && segments.size() == 2 && ours(segments.get(0))
					&& UUID.matcher(segments.get(1)).matches()
							? Optional.of("https://" + host + "/" + company + "/" + segments.get(1).toLowerCase(Locale.ROOT))
							: Optional.empty();
			case WORKABLE -> host.equals(host()) && segments.size() == 3 && ours(segments.get(0))
					&& segments.get(1).equals("j") && WORKABLE_CODE.matcher(segments.get(2)).matches()
							? Optional.of("https://" + host + "/" + company + "/j/" + segments.get(2)) : Optional.empty();
			case SMARTRECRUITERS -> SMARTRECRUITERS_HOSTS.contains(host) && segments.size() == 2
					&& ours(segments.get(0)) && SMARTRECRUITERS_POSTING.matcher(segments.get(1)).matches()
							? Optional.of("https://jobs.smartrecruiters.com/" + company + "/" + segments.get(1))
							: Optional.empty();
			case WORKDAY -> workdayPosting(host, segments);
		};
		return posting.filter(address -> address.length() <= MAX_LENGTH);
	}

	/** A name for the company, from its part of the address; the student can change it. */
	String suggestedName() {
		var words = new ArrayList<String>();
		for (String word : company.split("[-_.]+")) {
			if (!word.isEmpty()) {
				words.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
			}
		}
		return String.join(" ", words);
	}

	private Optional<String> workdayPosting(String host, List<String> segments) {
		List<String> rest = withoutLocale(segments);
		if (!host.equals(host()) || rest.size() < 3 || !rest.get(0).equalsIgnoreCase(site)
				|| !rest.get(1).equals("job")) {
			return Optional.empty();
		}
		return Optional.of("https://" + host + "/" + site + "/" + String.join("/", rest.subList(1, rest.size())));
	}

	private boolean ours(String segment) {
		return segment.equalsIgnoreCase(company);
	}

	private String host() {
		return URI.create(url).getHost();
	}

	private static Optional<CareerPageAddress> page(Platform platform, String url, String company, String site) {
		if (url.length() > MAX_LENGTH || !SEGMENT.matcher(company).matches()
				|| !(site.isEmpty() || SEGMENT.matcher(site).matches())) {
			return Optional.empty();
		}
		return Optional.of(new CareerPageAddress(platform, url, company, site));
	}

	private static List<String> withoutLocale(List<String> segments) {
		return !segments.isEmpty() && LOCALE.matcher(segments.get(0)).matches()
				? segments.subList(1, segments.size()) : segments;
	}

	/** An absolute https (or http) address on the default port, resolved against {@code base} when relative. */
	private static Optional<URI> uri(String value, URI base) {
		if (value == null || value.isBlank() || value.length() > MAX_LENGTH * 2) {
			return Optional.empty();
		}
		URI uri;
		try {
			uri = new URI(value.strip());
			if (base != null && !uri.isAbsolute()) {
				uri = base.resolve(uri);
			}
		}
		catch (URISyntaxException | IllegalArgumentException ex) {
			return Optional.empty();
		}
		String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
		if (!(scheme.equals("https") || scheme.equals("http")) || uri.getHost() == null || uri.getPort() != -1
				|| uri.getRawUserInfo() != null) {
			return Optional.empty();
		}
		return Optional.of(uri);
	}

	private static List<String> segments(URI uri) {
		String path = uri.getPath() == null ? "" : uri.getPath();
		return Arrays.stream(path.split("/")).filter(s -> !s.isEmpty()).toList();
	}

}
