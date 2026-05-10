package com.hyso.notifier.domain.notification.outbox.repository;

import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository {

    NotificationOutbox save(NotificationOutbox notificationOutbox);

    Optional<NotificationOutbox> findByIdempotencyKey(String idempotencyKey);

    List<NotificationOutbox> findClaimableForUpdate(LocalDateTime now, int batchSize);

    List<NotificationOutbox> findRecoverableForUpdate(LocalDateTime cutoff, int batchSize);

    boolean saveIfLeaseMatched(NotificationOutbox outbox, LocalDateTime claimedProcessingStartedAt);

    int deleteSentOlderThan(LocalDateTime cutoff, int batchSize);

    int deleteFailedOlderThan(LocalDateTime cutoff, int batchSize);
}
