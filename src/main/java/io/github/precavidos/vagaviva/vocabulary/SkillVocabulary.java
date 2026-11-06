package io.github.precavidos.vagaviva.vocabulary;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import tools.jackson.databind.json.JsonMapper;

/**
 * An archetype's closed vocabulary of skills and named technologies. It only
 * exists in a valid state: any inconsistency prevents its creation.
 */
public final class SkillVocabulary {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final String archetype;

	private final Map<String, Skill> skills;

	private final List<NamedTechnology> technologies;

	private final Map<String, NamedTechnology> technologyById;

	private final List<RejectedTerm> rejectedTerms;

	private final Map<String, Pattern> patterns = new ConcurrentHashMap<>();

	private SkillVocabulary(VocabularyFile file) {
		this.archetype = file.archetype();
		this.technologies = file.technologies() == null ? List.of() : List.copyOf(file.technologies());
		var byId = new HashMap<String, NamedTechnology>();
		this.technologies.forEach(t -> byId.putIfAbsent(t.id(), t));
		this.technologyById = byId;
		this.rejectedTerms = file.rejectedTerms() == null ? List.of() : List.copyOf(file.rejectedTerms());
		var problems = new ArrayList<String>();
		this.skills = index(file.skills(), problems);
		validateTechnologies(problems);
		validateTerms(problems);
		if (!problems.isEmpty()) {
			throw new InvalidVocabularyException(archetype, problems);
		}
	}

	public static SkillVocabulary read(InputStream input) {
		return new SkillVocabulary(JSON.readValue(input, VocabularyFile.class));
	}

	public static SkillVocabulary fromClasspath(String path) {
		try (InputStream input = SkillVocabulary.class.getClassLoader().getResourceAsStream(path)) {
			if (input == null) {
				throw new IllegalArgumentException("Vocabulary not found on the classpath: " + path);
			}
			return read(input);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public String archetype() {
		return archetype;
	}

	public List<Skill> skills() {
		return List.copyOf(skills.values());
	}

	public Optional<Skill> skill(String id) {
		return Optional.ofNullable(skills.get(id));
	}

	/** The skill's label, or the id itself when the vocabulary does not know it. */
	public String skillLabel(String id) {
		Skill skill = skills.get(id);
		return skill == null ? id : skill.label();
	}

	/** The technology's name, or the id itself when the vocabulary does not know it. */
	public String technologyName(String id) {
		NamedTechnology technology = technologyById.get(id);
		return technology == null ? id : technology.name();
	}

	public List<NamedTechnology> technologies() {
		return technologies;
	}

	public List<RejectedTerm> rejectedTerms() {
		return rejectedTerms;
	}

	/**
	 * Recognizes the vocabulary's technologies and skills in a text. A term only
	 * matches as a whole word ("Java" does not match "JavaScript"), ignoring case,
	 * accents and extra whitespace.
	 */
	public Recognition recognize(String text) {
		String normalized = TextNormalizer.normalize(text);
		var evidence = new LinkedHashMap<String, List<String>>();
		skills.values().forEach(skill -> {
			List<String> matched = match(skill.terms(), normalized);
			if (!matched.isEmpty()) {
				evidence.computeIfAbsent(skill.id(), id -> new ArrayList<>()).addAll(matched);
			}
		});
		var recognizedTechnologies = new ArrayList<Recognition.RecognizedTechnology>();
		for (NamedTechnology technology : technologies) {
			List<String> matched = match(technology.terms(), normalized);
			if (matched.isEmpty()) {
				continue;
			}
			recognizedTechnologies.add(new Recognition.RecognizedTechnology(technology, matched));
			technology.skills().forEach(id -> evidence.computeIfAbsent(id, key -> new ArrayList<>()).addAll(matched));
		}
		var recognizedSkills = skills.values()
			.stream()
			.filter(skill -> evidence.containsKey(skill.id()))
			.map(skill -> new Recognition.RecognizedSkill(skill, evidence.get(skill.id())))
			.toList();
		return new Recognition(recognizedTechnologies, recognizedSkills);
	}

	private List<String> match(List<String> terms, String normalizedText) {
		return terms.stream().filter(term -> patternFor(term).matcher(normalizedText).find()).toList();
	}

	private Pattern patternFor(String term) {
		return patterns.computeIfAbsent(term, t -> Pattern
			.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(TextNormalizer.normalize(t)) + "(?![\\p{L}\\p{N}])"));
	}

	private static Map<String, Skill> index(List<Skill> list, List<String> problems) {
		var index = new LinkedHashMap<String, Skill>();
		for (Skill skill : list == null ? List.<Skill>of() : list) {
			if (index.putIfAbsent(skill.id(), skill) != null) {
				problems.add("duplicate skill: " + skill.id());
			}
			if (skill.label() == null || skill.label().isBlank()) {
				problems.add("skill " + skill.id() + " has no label");
			}
			if (skill.esco() != null && !skill.esco().hasValidUri()) {
				problems.add("skill " + skill.id() + " has an invalid ESCO URI");
			}
		}
		return index;
	}

	private void validateTechnologies(List<String> problems) {
		Set<String> ids = new HashSet<>();
		for (NamedTechnology technology : technologies) {
			if (!ids.add(technology.id())) {
				problems.add("duplicate technology: " + technology.id());
			}
			if (technology.terms().isEmpty()) {
				problems.add("technology " + technology.id() + " has no terms");
			}
			if (technology.skills().isEmpty()) {
				problems.add("technology " + technology.id() + " points to no skill");
			}
			for (String id : technology.skills()) {
				if (!skills.containsKey(id)) {
					problems.add("technology " + technology.id() + " points to an unknown skill: " + id);
				}
			}
		}
	}

	private void validateTerms(List<String> problems) {
		var rejected = new HashMap<String, RejectedTerm>();
		rejectedTerms.forEach(r -> rejected.put(TextNormalizer.normalize(r.term()), r));
		var owners = new HashMap<String, String>();
		skills.values().forEach(s -> validateTerms("skill " + s.id(), s.terms(), owners, rejected, problems));
		technologies.forEach(t -> validateTerms("technology " + t.id(), t.terms(), owners, rejected, problems));
	}

	private static void validateTerms(String owner, List<String> terms, Map<String, String> owners,
			Map<String, RejectedTerm> rejected, List<String> problems) {
		for (String term : terms) {
			String key = TextNormalizer.normalize(term);
			RejectedTerm rejectedTerm = rejected.get(key);
			if (rejectedTerm != null) {
				problems.add("rejected term in use: " + term + " (" + rejectedTerm.reason() + ")");
			}
			String previousOwner = owners.putIfAbsent(key, owner);
			if (previousOwner != null && !previousOwner.equals(owner)) {
				problems.add("term \"" + term + "\" appears in more than one entry");
			}
		}
	}

	private record VocabularyFile(String archetype, List<Skill> skills, List<NamedTechnology> technologies,
			List<RejectedTerm> rejectedTerms) {
	}

}
