package com.hyso.notifier.infrastructure.notification.persistence;

import com.hyso.notifier.domain.notification.Notification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JpaNotificationRepository extends ListCrudRepository<Notification, Long> {

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.sentAt = :sentAt
            WHERE n.id = :id
            """)
    int markSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Notification n
            SET n.failedAt = :failedAt,
                n.failureReason = :failureReason
            WHERE n.id = :id
            """)
    int markFailed(
            @Param("id") Long id,
            @Param("failedAt") LocalDateTime failedAt,
            @Param("failureReason") String failureReason
    );
}
