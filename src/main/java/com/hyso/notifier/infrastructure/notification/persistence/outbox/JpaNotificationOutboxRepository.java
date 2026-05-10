package com.hyso.notifier.infrastructure.notification.persistence.outbox;

import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaNotificationOutboxRepository extends ListCrudRepository<NotificationOutbox, Long> {

    Optional<NotificationOutbox> findByIdempotencyKey(String idempotencyKey);

    @Query(
            value = """
                    SELECT *
                    FROM notification_outboxes
                    WHERE status IN ('PENDING', 'RETRY_PENDING')
                      AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
                    ORDER BY id ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<NotificationOutbox> findClaimableForUpdate(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );
}
