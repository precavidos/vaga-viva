package io.github.precavidos.vagaviva.jobposting;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.precavidos.vagaviva.vocabulary.Skill;

@RestController
@RequestMapping("/api/job-postings")
class JobPostingController {

	private final JobPostingExtractor extractor;

	JobPostingController(JobPostingExtractor extractor) {
		this.extractor = extractor;
	}

	@PostMapping("/extraction")
	ExtractionResponse extract(@Valid @RequestBody ExtractionRequest request) {
		return ExtractionResponse.from(extractor.extract(request.text()));
	}

	record ExtractionRequest(@NotBlank @Size(max = JobPostingExtractor.MAX_LENGTH) String text) {
	}

	record ExtractionResponse(String archetype, List<SeniorityLevel> seniorityLevels,
			List<TechnologyDto> technologies, List<SkillDto> skills) {

		static ExtractionResponse from(ExtractedJobPosting posting) {
			var recognition = posting.recognition();
			return new ExtractionResponse(posting.archetype(), posting.seniorityLevels(),
					recognition.technologies()
						.stream()
						.map(t -> new TechnologyDto(t.technology().id(), t.technology().name(), t.matchedTerms()))
						.toList(),
					recognition.skills().stream().map(s -> SkillDto.from(s.skill(), s.evidence())).toList());
		}

	}

	record TechnologyDto(String id, String name, List<String> matchedTerms) {
	}

	record SkillDto(String id, String label, Skill.Source source, String escoUri, List<String> evidence) {

		static SkillDto from(Skill skill, List<String> evidence) {
			return new SkillDto(skill.id(), skill.label(), skill.source(),
					skill.esco() == null ? null : skill.esco().uri(), evidence);
		}

	}

}
