package com.hyso.notifier.infrastructure.notification.persistence.outbox;

import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query(
            value = """
                    SELECT *
                    FROM notification_outboxes
                    WHERE status = 'PROCESSING'
                      AND processing_started_at < :cutoff
                    ORDER BY processing_started_at ASC
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<NotificationOutbox> findRecoverableForUpdate(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("batchSize") int batchSize
    );

    @Modifying(clearAutomatically = true)
    @Query(
            value = """
                    UPDATE notification_outboxes
                    SET status = :status,
                        processing_lease_state = :leaseState,
                        processing_started_at = :processingStartedAt,
                        next_attempt_at = :nextAttemptAt,
                        dispatched_at = :dispatchedAt,
                        failed_at = :failedAt,
                        failure_reason = :failureReason,
                        updated_at = :updatedAt
                    WHERE id = :id
                      AND status = 'PROCESSING'
                      AND processing_lease_state = 'CLAIMED'
                      AND processing_started_at = :claimedProcessingStartedAt
                    """,
            nativeQuery = true
    )
    int updateIfLeaseMatched(
            @Param("id") Long id,
            @Param("claimedProcessingStartedAt") LocalDateTime claimedProcessingStartedAt,
            @Param("status") String status,
            @Param("leaseState") String leaseState,
            @Param("processingStartedAt") LocalDateTime processingStartedAt,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            @Param("dispatchedAt") LocalDateTime dispatchedAt,
            @Param("failedAt") LocalDateTime failedAt,
            @Param("failureReason") String failureReason,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(clearAutomatically = true)
    @Query(
            value = """
                    DELETE FROM notification_outboxes
                    WHERE status = 'DISPATCHED'
                      AND dispatched_at < :cutoff
                    LIMIT :batchSize
                    """,
            nativeQuery = true
    )
    int deleteDispatchedOlderThan(@Param("cutoff") LocalDateTime cutoff, @Param("batchSize") int batchSize);

    @Modifying(clearAutomatically = true)
    @Query(
            value = """
                    DELETE FROM notification_outboxes
                    WHERE status = 'FAILED'
                      AND failed_at < :cutoff
                    LIMIT :batchSize
                    """,
            nativeQuery = true
    )
    int deleteFailedOlderThan(@Param("cutoff") LocalDateTime cutoff, @Param("batchSize") int batchSize);
}
