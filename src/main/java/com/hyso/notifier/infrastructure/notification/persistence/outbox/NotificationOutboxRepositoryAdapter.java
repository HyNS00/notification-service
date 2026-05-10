package com.hyso.notifier.infrastructure.notification.persistence.outbox;

import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationOutboxRepositoryAdapter implements NotificationOutboxRepository {

    private final JpaNotificationOutboxRepository jpaNotificationOutboxRepository;

    @Override
    public NotificationOutbox save(NotificationOutbox notificationOutbox) {
        return jpaNotificationOutboxRepository.save(notificationOutbox);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationOutbox> findByIdempotencyKey(String idempotencyKey) {
        return jpaNotificationOutboxRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<NotificationOutbox> findClaimableForUpdate(LocalDateTime now, int batchSize) {
        return jpaNotificationOutboxRepository.findClaimableForUpdate(now, batchSize);
    }
}
