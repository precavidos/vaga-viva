package io.github.precavidos.vagaviva.archetype;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.precavidos.vagaviva.archetype.ArchetypeFit.Status;
import io.github.precavidos.vagaviva.vocabulary.TermMatcher;
import io.github.precavidos.vagaviva.vocabulary.TextNormalizer;
import tools.jackson.databind.json.JsonMapper;

/**
 * The archetypes the Vaga Viva offers and the languages and frameworks it can
 * tell apart in a posting, including those no archetype practices. With it the
 * posting gets the archetype that covers most of its stack, by fixed rules
 * (ADR 0002), and the student learns what the project leaves out instead of
 * silently getting another stack. It only exists in a valid state.
 */
public final class ArchetypeCatalog {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final List<StackTechnology> technologies;

	private final List<Archetype> archetypes;

	private final TermMatcher matcher = new TermMatcher();

	private ArchetypeCatalog(CatalogFile file) {
		this.technologies = file.technologies() == null ? List.of() : List.copyOf(file.technologies());
		this.archetypes = file.archetypes() == null ? List.of() : List.copyOf(file.archetypes());
		var problems = new ArrayList<String>();
		validate(problems);
		if (!problems.isEmpty()) {
			throw new InvalidArchetypeCatalogException(problems);
		}
	}

	public static ArchetypeCatalog read(InputStream input) {
		return new ArchetypeCatalog(JSON.readValue(input, CatalogFile.class));
	}

	public static ArchetypeCatalog fromClasspath(String path) {
		try (InputStream input = ArchetypeCatalog.class.getClassLoader().getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalArgumentException("Archetype catalog not found on the classpath: " + path);
			}
			return read(input);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** The archetype used when the posting names no language with an archetype: the first one listed. */
	public String defaultArchetype() {
		return archetypes.getFirst().id();
	}

	/**
	 * Chooses the archetype for a posting. Candidates are the archetypes whose
	 * language the posting names, directly or through one of its frameworks;
	 * the one practicing most of what the posting names wins, ties going to
	 * the first listed.
	 */
	public ArchetypeFit match(String text) {
		String normalized = TextNormalizer.normalize(text);
		List<StackTechnology> named = technologies.stream()
			.filter(t -> !matcher.match(t.terms(), normalized).isEmpty())
			.toList();
		if (named.isEmpty()) {
			return new ArchetypeFit(defaultArchetype(), Status.NOT_STATED, List.of(), List.of());
		}
		Set<String> languages = new LinkedHashSet<>();
		named.forEach(t -> languages.add(t.isLanguage() ? t.id() : t.language()));
		Optional<Archetype> chosen = archetypes.stream()
			.filter(a -> a.languages().stream().anyMatch(languages::contains))
			.max(Comparator.comparingLong((Archetype a) -> named.stream().filter(a::covers).count())
				.thenComparing(a -> -archetypes.indexOf(a)));
		if (chosen.isEmpty()) {
			return new ArchetypeFit(defaultArchetype(), Status.NOT_COVERED, List.of(), names(named));
		}
		Archetype archetype = chosen.get();
		var covered = named.stream().filter(archetype::covers).toList();
		var uncovered = named.stream().filter(t -> !archetype.covers(t)).toList();
		return new ArchetypeFit(archetype.id(), uncovered.isEmpty() ? Status.COVERED : Status.PARTIAL,
				names(covered), names(uncovered));
	}

	private static List<String> names(List<StackTechnology> list) {
		return list.stream().map(StackTechnology::name).toList();
	}

	private void validate(List<String> problems) {
		Map<String, StackTechnology> byId = new LinkedHashMap<>();
		Map<String, String> termOwners = new HashMap<>();
		for (StackTechnology technology : technologies) {
			if (byId.putIfAbsent(technology.id(), technology) != null) {
				problems.add("duplicate technology: " + technology.id());
			}
			if (technology.terms().isEmpty()) {
				problems.add("technology " + technology.id() + " has no terms");
			}
			for (String term : technology.terms()) {
				String previous = termOwners.putIfAbsent(TextNormalizer.normalize(term), technology.id());
				if (previous != null && !previous.equals(technology.id())) {
					problems.add("term \"" + term + "\" appears in more than one technology");
				}
			}
		}
		for (StackTechnology technology : technologies) {
			if (!technology.isLanguage() && !isLanguage(byId.get(technology.language()))) {
				problems.add("technology " + technology.id() + " points to an unknown language: " + technology.language());
			}
		}
		if (archetypes.isEmpty()) {
			problems.add("no archetypes");
		}
		Set<String> archetypeIds = new HashSet<>();
		for (Archetype archetype : archetypes) {
			if (!archetypeIds.add(archetype.id())) {
				problems.add("duplicate archetype: " + archetype.id());
			}
			if (archetype.languages().isEmpty()) {
				problems.add("archetype " + archetype.id() + " has no language");
			}
			for (String language : archetype.languages()) {
				if (!isLanguage(byId.get(language))) {
					problems.add("archetype " + archetype.id() + " has an unknown language: " + language);
				}
			}
			for (String framework : archetype.frameworks()) {
				StackTechnology technology = byId.get(framework);
				if (technology == null || technology.isLanguage() || !archetype.languages().contains(technology.language())) {
					problems.add("archetype " + archetype.id() + " lists " + framework + ", which is not a framework of "
							+ String.join(" or ", archetype.languages()));
				}
			}
			if (isBlank(archetype.name()) || isBlank(archetype.templateRepository())
					|| isBlank(archetype.missionTestCommand()) || !archetype.missionTestCommand().contains("%s")) {
				problems.add("archetype " + archetype.id()
						+ " needs a name, a template repository and a mission test command with %s for the mission id");
			}
		}
	}

	private static boolean isLanguage(StackTechnology technology) {
		return technology != null && technology.isLanguage();
	}

	/**
	 * A programming language, or a framework of one, recognized only by its
	 * own terms.
	 *
	 * @param language for a framework, the id of its language; {@code null} for a language
	 */
	record StackTechnology(String id, String name, String language, List<String> terms) {

		StackTechnology {
			terms = terms == null ? List.of() : List.copyOf(terms);
		}

		boolean isLanguage() {
			return language == null;
		}

	}

	/**
	 * An archetype: its stack (the languages and frameworks its template
	 * repository practices) and what provisioning needs to create a student
	 * repository from it.
	 *
	 * @param name the stack as students read it, for example "Java e Spring Boot"
	 * @param templateRepository the template repository, as {@code owner/name}
	 * @param missionTestCommand the command that runs one mission's tests, with
	 * {@code %s} for the mission id
	 */
	public record Archetype(String id, String name, List<String> languages, List<String> frameworks,
			String templateRepository, String missionTestCommand) {

		public Archetype {
			languages = languages == null ? List.of() : List.copyOf(languages);
			frameworks = frameworks == null ? List.of() : List.copyOf(frameworks);
		}

		boolean covers(StackTechnology technology) {
			return technology.isLanguage() ? languages.contains(technology.id()) : frameworks.contains(technology.id());
		}

		/** The command a student runs to check one mission. */
		public String missionTestCommand(String missionId) {
			return missionTestCommand.formatted(missionId);
		}

	}

	/** The archetype with this id. */
	public Archetype archetype(String id) {
		return archetypes.stream()
			.filter(a -> a.id().equals(id))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown archetype: " + id));
	}

	/** Every archetype on offer, the default first. */
	public List<Archetype> archetypes() {
		return archetypes;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record CatalogFile(List<StackTechnology> technologies, List<Archetype> archetypes) {
	}

}
