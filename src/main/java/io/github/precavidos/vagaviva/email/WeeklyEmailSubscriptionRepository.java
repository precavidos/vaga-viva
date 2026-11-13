package io.github.precavidos.vagaviva.email;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface WeeklyEmailSubscriptionRepository extends JpaRepository<WeeklyEmailSubscription, String> {

	Optional<WeeklyEmailSubscription> findByUnsubscribeToken(String token);

	/** Subscriptions never sent, or last sent before the given instant. */
	@Query("select s from WeeklyEmailSubscription s where s.lastSentAt is null or s.lastSentAt < :sentBefore "
			+ "order by s.studentLogin")
	List<WeeklyEmailSubscription> findDue(@Param("sentBefore") Instant sentBefore);

	/** Marks the send on the row as it is now; does nothing when the student removed it meanwhile. */
	@Modifying
	@Transactional
	@Query("update WeeklyEmailSubscription s set s.lastSentAt = :now where s.studentLogin = :login")
	int markSent(@Param("login") String login, @Param("now") Instant now);

	/** Deletes by login; does nothing when the row is already gone. */
	@Modifying
	@Transactional
	@Query("delete from WeeklyEmailSubscription s where s.studentLogin = :login")
	int removeByLogin(@Param("login") String login);

}
