package io.github.precavidos.vagaviva.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.precavidos.vagaviva.github.GitHubApi;
import io.github.precavidos.vagaviva.github.GitHubApiException;
import io.github.precavidos.vagaviva.github.GitHubComment;
import io.github.precavidos.vagaviva.github.GitHubFile;
import io.github.precavidos.vagaviva.github.GitHubPullRequest;
import io.github.precavidos.vagaviva.github.InstallationToken;
import io.github.precavidos.vagaviva.plan.MissionCatalog;
import io.github.precavidos.vagaviva.vocabulary.SkillVocabulary;

class RepositorySnapshotsTests {

	private static final String REPO = "maria/vaga-viva-clinic-scheduling";

	private static final String PLAN = """
			{ "archetype": "java-spring", "domain": "clinic-scheduling", "missions": ["patients-api", "persistence"] }""";

	private static final MissionCatalog CATALOG = MissionCatalog.fromClasspath("missions/java-spring.json",
			SkillVocabulary.fromClasspath("vocabulary/java-spring.json"));

	private final GitHubApi github = mock(GitHubApi.class);

	private final FailedTestsSource failedTests = mock(FailedTestsSource.class);

	private final TestReport report = new TestReport(Set.of("patients-api"), List.of());

	private RepositorySnapshots snapshots;

	@BeforeEach
	void setUp() {
		var app = new GitHubAppProperties("1", "key", "secret", "vaga-viva");
		var tokens = new InstallationTokens(github, () -> "jwt", Clock.systemUTC());
		snapshots = new RepositorySnapshots(github, tokens, new StudentPlans(github, List.of(CATALOG)), failedTests, app);
		when(github.repositoryInstallation("jwt", REPO)).thenReturn(OptionalLong.of(99));
		when(github.createInstallationToken("jwt", 99))
			.thenReturn(new InstallationToken("ghs_99", Instant.now().plusSeconds(3600)));
		when(github.readFile("ghs_99", REPO, ".vaga-viva/plan.json")).thenReturn(Optional.of(new GitHubFile(PLAN, "sha")));
		when(failedTests.report("ghs_99", REPO, "abc123")).thenReturn(Optional.of(report));
	}

	@Test
	void takesPlanReportAndLastReview() {
		when(github.latestPullRequest("ghs_99", REPO)).thenReturn(Optional.of(new GitHubPullRequest(3, "abc123")));
		when(github.issueComments("ghs_99", REPO, 3)).thenReturn(List.of(
				new GitHubComment(1, "vaga-viva[bot]", true, ReviewTexts.comment("Primeira pergunta")),
				new GitHubComment(2, "maria", false, "/dica"),
				new GitHubComment(3, "vaga-viva[bot]", true, ReviewTexts.comment("  Segunda pergunta  "))));

		RepositorySnapshot snapshot = snapshots.take(REPO).orElseThrow();

		assertThat(snapshot.plan().missions()).extracting(m -> m.id()).containsExactly("patients-api", "persistence");
		assertThat(snapshot.report()).contains(report);
		assertThat(snapshot.lastReview()).contains("Segunda pergunta");
	}

	@Test
	void ignoresHintCommentsAndStudentComments() {
		when(github.latestPullRequest("ghs_99", REPO)).thenReturn(Optional.of(new GitHubPullRequest(3, "abc123")));
		when(github.issueComments("ghs_99", REPO, 3)).thenReturn(List.of(
				new GitHubComment(1, "vaga-viva[bot]", true, ReviewTexts.comment("A pergunta")),
				new GitHubComment(2, "vaga-viva[bot]", true, "<!-- vaga-viva:hint 1 -->\nUma dica"),
				new GitHubComment(3, "maria", false, ReviewTexts.comment("Copiei o marcador"))));

		assertThat(snapshots.take(REPO).orElseThrow().lastReview()).contains("A pergunta");
	}

	@Test
	void usesTheDefaultBranchWithoutPullRequests() {
		when(github.latestPullRequest("ghs_99", REPO)).thenReturn(Optional.empty());
		when(github.latestCommitSha("ghs_99", REPO)).thenReturn("abc123");

		RepositorySnapshot snapshot = snapshots.take(REPO).orElseThrow();

		verify(failedTests).report("ghs_99", REPO, "abc123");
		assertThat(snapshot.report()).contains(report);
		assertThat(snapshot.lastReview()).isEmpty();
	}

	@Test
	void emptyWhenTheAppIsNotInstalled() {
		when(github.repositoryInstallation("jwt", REPO)).thenReturn(OptionalLong.empty());

		assertThat(snapshots.take(REPO)).isEmpty();
	}

	@Test
	void emptyWhenTheRepositoryDoesNotExist() {
		when(github.readFile("ghs_99", REPO, ".vaga-viva/plan.json")).thenThrow(new GitHubApiException(404, "Not Found"));

		assertThat(snapshots.take(REPO)).isEmpty();
	}

	@Test
	void emptyWhenThereIsNoPlan() {
		when(github.readFile("ghs_99", REPO, ".vaga-viva/plan.json")).thenReturn(Optional.empty());

		assertThat(snapshots.take(REPO)).isEmpty();
	}

	@Test
	void isEnabledOnlyWithTheAppIdAndKey() {
		var tokens = new InstallationTokens(github, () -> "jwt", Clock.systemUTC());
		var plans = new StudentPlans(github, List.of(CATALOG));

		assertThat(snapshots.enabled()).isTrue();
		assertThat(new RepositorySnapshots(github, tokens, plans, failedTests,
				new GitHubAppProperties("", "key", "", "vaga-viva")).enabled()).isFalse();
		assertThat(new RepositorySnapshots(github, tokens, plans, failedTests,
				new GitHubAppProperties("1", " ", "", "vaga-viva")).enabled()).isFalse();
		assertThat(new RepositorySnapshots(github, tokens, plans, failedTests,
				new GitHubAppProperties("1", "key", "", "vaga-viva")).enabled()).isTrue();
	}

}
