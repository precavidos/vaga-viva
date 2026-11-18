package io.github.precavidos.vagaviva.classroom;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.precavidos.vagaviva.archetype.ArchetypeCatalog;
import io.github.precavidos.vagaviva.plan.Mission;
import io.github.precavidos.vagaviva.review.MissionTally;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * What the teacher sees: every student placed on a mission with a status computed on read, and one
 * student's card. The teacher sees names, missions, failing tests, reviewer questions and the hint count,
 * never code. Whoever is not the classroom's teacher gets the same answer as for a classroom that does not
 * exist, so the existence of a classroom is not revealed.
 */
@Service
class DashboardService {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final ClassroomRepository classrooms;

	private final EnrollmentRepository enrollments;

	private final EnrollmentProgressRepository progresses;

	private final ClassroomService classroomService;

	private final ClassroomProperties properties;

	private final Clock clock;

	private final ArchetypeCatalog archetypes;

	DashboardService(ClassroomRepository classrooms, EnrollmentRepository enrollments,
			EnrollmentProgressRepository progresses, ClassroomService classroomService,
			ClassroomProperties properties, Clock clock, ArchetypeCatalog archetypes) {
		this.classrooms = classrooms;
		this.enrollments = enrollments;
		this.progresses = progresses;
		this.classroomService = classroomService;
		this.properties = properties;
		this.clock = clock;
		this.archetypes = archetypes;
	}

	/** The dashboard of a classroom of this teacher; empty when it does not exist or belongs to someone else. */
	@Transactional(readOnly = true)
	Optional<Dashboard> dashboard(UUID classroomId, String teacherLogin) {
		return ownedBy(classroomId, teacherLogin).map(this::dashboardForSummary);
	}

	/** One student's card; empty when the classroom is not this teacher's or the student is not in it. */
	@Transactional(readOnly = true)
	Optional<StudentCard> student(UUID classroomId, String studentLogin, String teacherLogin) {
		return ownedBy(classroomId, teacherLogin).flatMap((classroom) -> enrollments
			.findByClassroomAndStudentLogin(classroom, studentLogin)
			.map((enrollment) -> card(classroom, enrollment)));
	}

	/** The dashboard without the teacher check, for the weekly summary, which runs without a signed-in user. */
	@Transactional(readOnly = true)
	Dashboard dashboardForSummary(Classroom classroom) {
		StoredPlan plan = StoredPlan.fromJson(classroom.getPlan());
		List<Mission> missions = missionsOf(plan);
		List<Enrollment> students = enrollments.findByClassroomOrderByEnrolledAt(classroom)
			.stream()
			.sorted(Comparator.comparing(Enrollment::getEnrolledAt).thenComparing(Enrollment::getStudentLogin))
			.toList();
		Map<UUID, EnrollmentProgress> progressById = progresses
			.findAllById(students.stream().map(Enrollment::getId).toList())
			.stream()
			.collect(Collectors.toMap(EnrollmentProgress::getId, Function.identity()));

		List<List<Card>> byMission = missions.stream().map((m) -> (List<Card>) new ArrayList<Card>()).toList();
		List<Card> completed = new ArrayList<>();
		Map<StudentStatus, Integer> counts = new EnumMap<>(StudentStatus.class);
		for (Enrollment enrollment : students) {
			EnrollmentProgress progress = progressById.get(enrollment.getId());
			Standing standing = standing(enrollment, progress, missions);
			Card card = new Card(enrollment.getStudentLogin(), enrollment.getDisplayName(), standing.status(),
					standing.daysOnMission(), standing.missionOrder(), standing.missionTitle(), standing.reason(),
					tests(missions, progress));
			counts.merge(card.status(), 1, Integer::sum);
			if (card.status() == StudentStatus.COMPLETED) {
				completed.add(card);
			}
			else if (card.missionOrder() >= 1) {
				byMission.get(card.missionOrder() - 1).add(card);
			}
			// Without any mission in the catalog nobody can be placed on a column: they are only counted.
		}

		List<Column> columns = new ArrayList<>();
		for (int i = 0; i < missions.size(); i++) {
			columns.add(new Column(i + 1, missions.get(i).id(), missions.get(i).title(), byMission.get(i)));
		}
		columns.add(new Column(null, null, "Concluído", completed));
		return new Dashboard(classroom.getId(), classroom.getName(), archetypes.archetype(plan.archetype()).name(),
				"/turma/" + classroom.getJoinCode(),
				new Counts(counts.getOrDefault(StudentStatus.STUCK, 0), counts.getOrDefault(StudentStatus.IDLE, 0),
						counts.getOrDefault(StudentStatus.ADVANCING, 0),
						counts.getOrDefault(StudentStatus.COMPLETED, 0)),
				columns);
	}

	private Optional<Classroom> ownedBy(UUID classroomId, String teacherLogin) {
		return classrooms.findById(classroomId)
			.filter((classroom) -> classroom.getTeacherLogin().equalsIgnoreCase(teacherLogin));
	}

	private StudentCard card(Classroom classroom, Enrollment enrollment) {
		List<Mission> missions = missionsOf(StoredPlan.fromJson(classroom.getPlan()));
		EnrollmentProgress progress = progresses.findById(enrollment.getId()).orElse(null);
		Standing standing = standing(enrollment, progress, missions);
		return new StudentCard(enrollment.getDisplayName(), enrollment.getStudentLogin(),
				"https://github.com/" + enrollment.getRepositoryFullName(), standing.missionOrder(),
				standing.missionTitle(), standing.status(), standing.reason(),
				progress == null ? List.of() : failingTests(progress.getFailingTests()),
				tests(missions, progress),
				progress == null ? null : progress.getLastReviewerQuestion(),
				hintsOn(missions, standing.missionOrder(), progress),
				progress == null ? null : progress.getLastPullRequestUrl());
	}

	/** The hints asked on the mission the card shows; 0 when none were, or when the row cannot be read. */
	private static int hintsOn(List<Mission> missions, int missionOrder, EnrollmentProgress progress) {
		if (progress == null || missionOrder < 1 || missionOrder > missions.size()) {
			return 0;
		}
		String json = progress.getHintsByMission();
		if (json == null || json.isBlank()) {
			return 0;
		}
		try {
			Map<String, Integer> hints = JSON.readValue(json, new TypeReference<Map<String, Integer>>() {
			});
			Integer count = hints == null ? null : hints.get(missions.get(missionOrder - 1).id());
			return count == null ? 0 : Math.max(0, count);
		}
		catch (RuntimeException e) {
			return 0;
		}
	}

	private List<Mission> missionsOf(StoredPlan plan) {
		Map<String, Mission> byId = classroomService.catalogOf(plan)
			.missions()
			.stream()
			.collect(Collectors.toMap(Mission::id, Function.identity()));
		return plan.missionIds().stream().map(byId::get).filter(Objects::nonNull).toList();
	}

	/** Where a student stands and why, all computed from the stored progress and the clock. */
	private Standing standing(Enrollment enrollment, EnrollmentProgress progress, List<Mission> missions) {
		Instant now = clock.instant();
		Instant enrolledAt = enrollment.getEnrolledAt();
		boolean completed = progress != null && progress.isCompleted();
		Instant since = progress == null || progress.getCurrentMissionSince() == null ? enrolledAt
				: progress.getCurrentMissionSince();
		Instant lastActivity = progress == null || progress.getLastActivityAt() == null ? enrolledAt
				: progress.getLastActivityAt();
		StudentStatus status = StudentStatus.of(completed, since, lastActivity, now, properties.stuckAfter(),
				properties.idleAfter());

		// Without missions there is no column and no order: 0 means "not on any mission".
		int index = missions.isEmpty() ? -1 : 0;
		if (progress != null && progress.getCurrentMissionId() != null) {
			// A current mission that the plan no longer has leaves the student on the first one, which is also
			// the column they are placed in, so the card and the reason agree.
			for (int i = 0; i < missions.size(); i++) {
				if (missions.get(i).id().equals(progress.getCurrentMissionId())) {
					index = i;
				}
			}
		}
		if (completed) {
			index = missions.size() - 1; // -1 when there are no missions
		}
		int daysOnMission = completed ? 0 : days(since, now);
		int missionOrder = index + 1;
		String missionTitle = completed ? "Projeto concluído" : index < 0 ? "" : missions.get(index).title();
		String reason = switch (status) {
			case STUCK -> dayCount(daysOnMission) + (missionOrder < 1 ? " sem avançar" : " na missão " + missionOrder) + ", "
					+ pullRequests(progress == null ? null : progress.getPullRequestsOnMission())
					+ " nesse período";
			case IDLE -> "sem atividade há " + dayCount(days(lastActivity, now));
			case ADVANCING -> daysOnMission == 0 ? "hoje" : "há " + dayCount(daysOnMission) + " na missão";
			case COMPLETED -> "todas as missões passando";
		};
		return new Standing(status, daysOnMission, missionOrder, missionTitle, reason);
	}

	/** One entry per mission of the plan, in plan order; the counts are null where there is no tally. */
	private static List<MissionTests> tests(List<Mission> missions, EnrollmentProgress progress) {
		Map<String, MissionTally> tallies = progress == null ? Map.of() : readTallies(progress.getTestTallies());
		List<MissionTests> tests = new ArrayList<>();
		for (int i = 0; i < missions.size(); i++) {
			String id = missions.get(i).id();
			MissionTally tally = tallies.get(id);
			tests.add(new MissionTests(i + 1, id, tally == null ? null : tally.passed(),
					tally == null ? null : tally.total()));
		}
		return List.copyOf(tests);
	}

	/** Like {@link #readList}: missing, broken or differently shaped JSON reads as an empty map. */
	private static Map<String, MissionTally> readTallies(String json) {
		if (json == null || json.isBlank()) {
			return Map.of();
		}
		try {
			Map<String, MissionTally> value = JSON.readValue(json, new TypeReference<Map<String, MissionTally>>() {
			});
			return value == null ? Map.of() : value;
		}
		catch (RuntimeException e) {
			return Map.of();
		}
	}

	/** Whole days, rounded down. */
	private static int days(Instant from, Instant to) {
		return (int) Math.max(0, Duration.between(from, to).toDays());
	}

	private static String dayCount(int days) {
		return days + (days == 1 ? " dia" : " dias");
	}

	private static String pullRequests(String json) {
		int count = readList(json, new TypeReference<List<Integer>>() {
		}).size();
		return count + (count == 1 ? " PR" : " PRs");
	}

	private static List<FailingTest> failingTests(String json) {
		return readList(json, new TypeReference<List<FailingTest>>() {
		}).stream().filter(Objects::nonNull).toList();
	}

	/** The stored JSON is ours, but a bad row must not break the whole dashboard: anything unexpected reads as empty. */
	private static <T> List<T> readList(String json, TypeReference<List<T>> type) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			List<T> value = JSON.readValue(json, type);
			return value == null ? List.of() : value;
		}
		catch (RuntimeException e) {
			return List.of();
		}
	}

	private record Standing(StudentStatus status, int daysOnMission, int missionOrder, String missionTitle,
			String reason) {
	}

	record Dashboard(UUID id, String name, String archetypeName, String inviteUrl, Counts counts, List<Column> columns) {
	}

	record Counts(int stuck, int idle, int advancing, int completed) {
	}

	record Column(Integer order, String missionId, String title, List<Card> students) {
	}

	record Card(String login, String displayName, StudentStatus status, int daysOnMission, int missionOrder,
			String missionTitle, String reason, List<MissionTests> tests) {
	}

	record StudentCard(String displayName, String login, String repositoryUrl, int missionOrder, String missionTitle,
			StudentStatus status, String reason, List<FailingTest> failingTests, List<MissionTests> tests,
			String lastReviewerQuestion, int hintsOnMission, String lastPullRequestUrl) {
	}

	record MissionTests(int order, String missionId, Integer passed, Integer total) {
	}

	record FailingTest(String name, String message) {
	}

}
