package io.github.precavidos.vagaviva.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import io.github.precavidos.vagaviva.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = NONE)
@TestPropertySource(properties = "logging.level.org.hibernate.orm.jdbc.error=OFF")
class WeeklyEmailSubscriptionRepositoryTests {

	private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

	@Autowired
	WeeklyEmailSubscriptionRepository subscriptions;

	@Autowired
	EntityManager entityManager;

	@Test
	void reconsentingSwitchesTheRepositoryAndClearsLastSent() {
		WeeklyEmailSubscription row = subscription("maria", "maria/vaga-viva-a", "t1");
		row.sentAt(NOW);
		subscriptions.saveAndFlush(row);

		row.consent("maria@example.com", "maria/vaga-viva-b", "t2", NOW.plusSeconds(60));
		subscriptions.saveAndFlush(row);
		entityManager.clear();

		WeeklyEmailSubscription found = subscriptions.findById("maria").orElseThrow();
		assertThat(found.getRepositoryFullName()).isEqualTo("maria/vaga-viva-b");
		assertThat(found.getUnsubscribeToken()).isEqualTo("t2");
		assertThat(found.getLastSentAt()).isNull();
		assertThat(subscriptions.findDue(NOW)).extracting(WeeklyEmailSubscription::getStudentLogin)
			.contains("maria");
	}

	@Test
	void findDueSkipsRecentSends() {
		WeeklyEmailSubscription recent = subscription("ana", "ana/vaga-viva-a", "ta");
		recent.sentAt(NOW.minus(3, ChronoUnit.DAYS));
		WeeklyEmailSubscription old = subscription("bia", "bia/vaga-viva-a", "tb");
		old.sentAt(NOW.minus(7, ChronoUnit.DAYS));
		subscriptions.saveAllAndFlush(java.util.List.of(recent, old));

		assertThat(subscriptions.findDue(NOW.minus(6, ChronoUnit.DAYS)))
			.extracting(WeeklyEmailSubscription::getStudentLogin)
			.containsExactly("bia");
	}

	@Test
	void findsByUnsubscribeToken() {
		subscriptions.saveAndFlush(subscription("maria", "maria/vaga-viva-a", "t1"));

		assertThat(subscriptions.findByUnsubscribeToken("t1")).isPresent();
		assertThat(subscriptions.findByUnsubscribeToken("nope")).isEmpty();
	}

	@Test
	void tokensAreUnique() {
		subscriptions.saveAndFlush(subscription("ana", "ana/vaga-viva-a", "same"));

		assertThatThrownBy(() -> subscriptions.saveAndFlush(subscription("bia", "bia/vaga-viva-a", "same")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private static WeeklyEmailSubscription subscription(String login, String repository, String token) {
		WeeklyEmailSubscription row = new WeeklyEmailSubscription(login);
		row.consent(login + "@example.com", repository, token, NOW);
		return row;
	}

}
