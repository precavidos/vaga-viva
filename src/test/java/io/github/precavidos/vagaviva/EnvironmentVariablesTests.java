package io.github.precavidos.vagaviva;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import io.github.precavidos.vagaviva.classroom.ClassroomProperties;
import io.github.precavidos.vagaviva.email.WeeklyEmailProperties;

/**
 * The variables named in {@code docs/deploy.md} reach the settings they are meant for. A key nested under the
 * wrong parent in {@code application.yaml} would leave the setting blank in production while every test that
 * sets the property directly still passes.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "WEEKLY_EMAIL_SECRET=weekly-from-env", "CLASSROOM_SUMMARY_SECRET=summary-from-env" })
class EnvironmentVariablesTests {

	@Autowired
	WeeklyEmailProperties weeklyEmail;

	@Autowired
	ClassroomProperties classroom;

	@Test
	void secretsComeFromTheirVariables() {
		assertThat(weeklyEmail.secret()).isEqualTo("weekly-from-env");
		assertThat(classroom.summarySecret()).isEqualTo("summary-from-env");
	}

}
