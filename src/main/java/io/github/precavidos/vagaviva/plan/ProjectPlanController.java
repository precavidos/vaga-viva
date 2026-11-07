package io.github.precavidos.vagaviva.plan;

import java.util.List;
import java.util.stream.IntStream;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.precavidos.vagaviva.archetype.ArchetypeFit;
import io.github.precavidos.vagaviva.jobposting.ExtractedJobPosting;
import io.github.precavidos.vagaviva.jobposting.JobPostingExtractor;
import io.github.precavidos.vagaviva.jobposting.ReceivesJobPostingText;
import io.github.precavidos.vagaviva.jobposting.SeniorityLevel;

@RestController
@ReceivesJobPostingText
@RequestMapping("/api/project-plans")
class ProjectPlanController {

	private final JobPostingExtractor extractor;

	private final ProjectPlanner planner;

	ProjectPlanController(JobPostingExtractor extractor, ProjectPlanner planner) {
		this.extractor = extractor;
		this.planner = planner;
	}

	@PostMapping
	PlanResponse plan(@Valid @RequestBody PlanRequest request) {
		ExtractedJobPosting posting = extractor.extract(request.text());
		return PlanResponse.from(planner.plan(posting));
	}

	record PlanRequest(@NotBlank @Size(max = JobPostingExtractor.MAX_LENGTH) String text) {
	}

	record PlanResponse(String archetype, ArchetypeFit archetypeFit, FictionalDomain domain, SeniorityLevel seniorityLevel, int difficultyCap,
			List<MissionDto> missions) {

		static PlanResponse from(ProjectPlan plan) {
			var missions = plan.missions();
			return new PlanResponse(plan.archetype(), plan.archetypeFit(), plan.domain(), plan.seniorityLevel(), plan.difficultyCap(),
					IntStream.range(0, missions.size()).mapToObj(i -> MissionDto.from(i + 1, missions.get(i))).toList());
		}

	}

	record MissionDto(int order, String id, String title, String summary, int difficulty,
			PlannedMission.Reason reason, List<String> matchedSkills) {

		static MissionDto from(int order, PlannedMission planned) {
			Mission m = planned.mission();
			return new MissionDto(order, m.id(), m.title(), m.summary(), m.difficulty(), planned.reason(),
					planned.matchedSkills());
		}

	}

}
