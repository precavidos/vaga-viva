package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubApiException;
import io.github.precavidos.vagaviva.github.GitHubArtifact;
import io.github.precavidos.vagaviva.github.GitHubWorkflowRun;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.review.ReviewContext.FailedTest;
import io.github.precavidos.vagaviva.review.ReviewContext.StudentPlan;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

/** Reading failed tests from the JUnit reports both templates upload. */
class JunitReportsTests {

	/** Shaped like Surefire's report in the Java template: one file per test class. */
	private static final String SUREFIRE = """
			<?xml version="1.0" encoding="UTF-8"?>
			<testsuite name="io.github.precavidos.clinic.missions.PatientsApiTest" tests="3" failures="2" errors="0" skipped="0">
			  <testcase name="createsAPatient" classname="Missão patients-api: cadastro de pacientes" time="0.01">
			    <failure message="[esperava status 201 ao cadastrar um paciente em POST /patients (missão patients-api); POST /patients respondeu 404 com corpo: ] &#10;expected: 201&#10; but was: 404" type="org.opentest4j.AssertionFailedError">stack</failure>
			  </testcase>
			  <testcase name="unknownPatientIsNotFound" classname="Missão patients-api: cadastro de pacientes" time="0.01">
			    <failure message="[esperava 404 para paciente inexistente] expected: 404 but was: 500" type="AssertionFailedError"/>
			  </testcase>
			  <testcase name="readsAPatient" classname="Missão patients-api: cadastro de pacientes" time="0.01"/>
			</testsuite>
			""";

	/** Shaped like jest-junit's single report in the TypeScript template. */
	private static final String JEST = """
			<?xml version="1.0" encoding="UTF-8"?>
			<testsuites name="jest tests" tests="4" failures="2" errors="0">
			  <testsuite name="Missão scheduling-rules: regras de agendamento" tests="2" failures="1" skipped="0">
			    <testcase classname="Missão scheduling-rules: regras de agendamento" name="recusa com 422 uma consulta no passado" time="0.07">
			      <failure>Error: Custom message:
			  esperava recusar consulta no passado com status 422

			expect(received).toBe(expected)
			    at expectStatus (test/support/api.ts:105:65)</failure>
			    </testcase>
			    <testcase classname="Missão scheduling-rules: regras de agendamento" name="aceita consultas encostadas" time="0.02"/>
			  </testsuite>
			  <testsuite name="Missão 'bug-hunt' fora do seu plano (.vaga-viva/plan.json) — Missão bug-hunt: caça ao bug" tests="1" skipped="1">
			    <testcase classname="Missão 'bug-hunt' fora do seu plano (.vaga-viva/plan.json) — Missão bug-hunt: caça ao bug" name="duração" time="0">
			      <skipped/>
			    </testcase>
			  </testsuite>
			  <testsuite name="Aplicação" tests="1" failures="1">
			    <testcase classname="Aplicação" name="sobe" time="0.1">
			      <failure>Error: a aplicação não subiu</failure>
			    </testcase>
			  </testsuite>
			</testsuites>
			""";

	private static final MissionCatalog CATALOG = MissionCatalog.fromClasspath("missions/java-spring.json",
			SkillVocabulary.fromClasspath("vocabulary/java-spring.json"));

	@Test
	void readsTheJavaTemplatesSurefireReports() {
		List<FailedTest> failed = JunitReports
			.failedTests(zip(Map.of("TEST-io.github.precavidos.clinic.missions.PatientsApiTest.xml", SUREFIRE,
					"io.github.precavidos.clinic.missions.PatientsApiTest.txt", "not xml")));

		assertThat(failed).containsExactly(
				new FailedTest("patients-api", "createsAPatient",
						"esperava status 201 ao cadastrar um paciente em POST /patients (missão patients-api); POST /patients respondeu 404 com corpo:"),
				new FailedTest("patients-api", "unknownPatientIsNotFound", "esperava 404 para paciente inexistente"));
	}

	@Test
	void readsTheTypeScriptTemplatesJestReportAndIgnoresSkippedAndNonMissionTests() {
		List<FailedTest> failed = JunitReports.failedTests(zip(Map.of("junit.xml", JEST)));

		assertThat(failed).containsExactly(new FailedTest("scheduling-rules", "recusa com 422 uma consulta no passado",
				"esperava recusar consulta no passado com status 422"));
	}

	@Test
	void capsLongMessages() {
		String report = SUREFIRE.replace("esperava 404 para paciente inexistente", "x".repeat(2_000));

		List<FailedTest> failed = JunitReports.failedTests(zip(Map.of("TEST-a.xml", report)));

		assertThat(failed.get(1).message()).hasSize(JunitReports.MAX_MESSAGE_LENGTH + 1).endsWith("…");
	}

	@Test
	void refusesReportsWithADoctypeSoNoExternalEntityIsResolved() {
		String xxe = """
				<?xml version="1.0"?>
				<!DOCTYPE testsuite [ <!ENTITY secret SYSTEM "file:///etc/passwd"> ]>
				<testsuite><testcase classname="Missão patients-api: x" name="t"><failure message="&secret;"/></testcase></testsuite>
				""";

		assertThatThrownBy(() -> JunitReports.failedTests(zip(Map.of("TEST-a.xml", xxe))))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void refusesAZipThatIsNotOne() {
		assertThat(JunitReports.failedTests("not a zip".getBytes(StandardCharsets.UTF_8))).isEmpty();
	}

	@Test
	void findsTheReportOfTheNewestFinishedVagaVivaRunAndKeepsThePlansMissionsInPlanOrder() {
		GitHubApi github = mock(GitHubApi.class);
		when(github.workflowRuns("token", "maria/repo", "abc")).thenReturn(List.of(
				new GitHubWorkflowRun(3, "Vaga Viva", "in_progress", null, "abc"),
				new GitHubWorkflowRun(2, "CI", "completed", "success", "abc"),
				new GitHubWorkflowRun(1, "Vaga Viva", "completed", "failure", "abc")));
		when(github.runArtifacts("token", "maria/repo", 1))
			.thenReturn(List.of(new GitHubArtifact(10, "other", false, 100), new GitHubArtifact(11, "junit-report", false, 2_000)));
		when(github.downloadArtifact("token", "maria/repo", 11))
			.thenReturn(zip(Map.of("junit.xml", JEST, "TEST-a.xml", SUREFIRE)));
		StudentPlan plan = plan("scheduling-rules", "patients-api");

		var failed = new JunitFailedTests(github).failedTests("token", "maria/repo", "abc", plan);

		assertThat(failed).hasValueSatisfying(tests -> assertThat(tests).extracting(FailedTest::missionId)
			.containsExactly("scheduling-rules", "patients-api", "patients-api"));
	}

	@Test
	void dropsFailedTestsOfMissionsOutsideThePlan() {
		GitHubApi github = finishedRunWith(zip(Map.of("TEST-a.xml", SUREFIRE)));

		var failed = new JunitFailedTests(github).failedTests("token", "maria/repo", "abc", plan("scheduling-rules"));

		assertThat(failed).hasValue(List.of());
	}

	@Test
	void knowsNothingWhileTheRunIsNotFinishedOrHasNoReport() {
		GitHubApi running = mock(GitHubApi.class);
		when(running.workflowRuns(anyString(), anyString(), anyString()))
			.thenReturn(List.of(new GitHubWorkflowRun(1, "Vaga Viva", "in_progress", null, "abc")));
		assertThat(new JunitFailedTests(running).failedTests("token", "maria/repo", "abc", plan("patients-api")))
			.isEmpty();
		verify(running, never()).runArtifacts(anyString(), anyString(), anyLong());

		GitHubApi noReport = mock(GitHubApi.class);
		when(noReport.workflowRuns(anyString(), anyString(), anyString()))
			.thenReturn(List.of(new GitHubWorkflowRun(1, "Vaga Viva", "completed", "failure", "abc")));
		when(noReport.runArtifacts("token", "maria/repo", 1))
			.thenReturn(List.of(new GitHubArtifact(11, "junit-report", true, 2_000),
					new GitHubArtifact(12, "surefire-reports", false, JunitFailedTests.MAX_ARTIFACT_BYTES + 1)));
		assertThat(new JunitFailedTests(noReport).failedTests("token", "maria/repo", "abc", plan("patients-api")))
			.isEmpty();
		verify(noReport, never()).downloadArtifact(anyString(), anyString(), anyLong());
	}

	@Test
	void knowsNothingWhenGitHubFailsOrTheReportIsUnreadable() {
		GitHubApi failing = mock(GitHubApi.class);
		when(failing.workflowRuns(anyString(), anyString(), anyString())).thenThrow(new GitHubApiException(403, "no"));
		assertThat(new JunitFailedTests(failing).failedTests("token", "maria/repo", "abc", plan("patients-api")))
			.isEmpty();

		GitHubApi broken = finishedRunWith(zip(Map.of("TEST-a.xml", "<testsuite><unclosed>")));
		assertThat(new JunitFailedTests(broken).failedTests("token", "maria/repo", "abc", plan("patients-api")))
			.isEmpty();
	}

	private static GitHubApi finishedRunWith(byte[] zip) {
		GitHubApi github = mock(GitHubApi.class);
		when(github.workflowRuns(anyString(), anyString(), anyString()))
			.thenReturn(List.of(new GitHubWorkflowRun(1, "Vaga Viva", "completed", "failure", "abc")));
		when(github.runArtifacts("token", "maria/repo", 1))
			.thenReturn(List.of(new GitHubArtifact(11, "surefire-reports", false, 2_000)));
		when(github.downloadArtifact("token", "maria/repo", 11)).thenReturn(zip);
		return github;
	}

	private static StudentPlan plan(String... missionIds) {
		var ids = List.of(missionIds);
		var missions = ids.stream()
			.map(id -> CATALOG.missions().stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow())
			.toList();
		return new StudentPlan("java-spring", "clinic-scheduling", missions);
	}

	private static byte[] zip(Map<String, String> files) {
		var bytes = new ByteArrayOutputStream();
		try (var zip = new ZipOutputStream(bytes)) {
			for (var file : files.entrySet()) {
				zip.putNextEntry(new ZipEntry(file.getKey()));
				zip.write(file.getValue().getBytes(StandardCharsets.UTF_8));
				zip.closeEntry();
			}
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
		return bytes.toByteArray();
	}

}
