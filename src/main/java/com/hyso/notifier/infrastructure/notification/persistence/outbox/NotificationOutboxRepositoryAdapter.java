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

    @Override
    @Transactional
    public boolean saveIfLeaseMatched(NotificationOutbox outbox, LocalDateTime claimedProcessingStartedAt) {
        validate(outbox, claimedProcessingStartedAt);
        int updated = jpaNotificationOutboxRepository.updateIfLeaseMatched(
                outbox.getId(),
                claimedProcessingStartedAt,
                outbox.getStatus().name(),
                outbox.getProcessingLeaseState().name(),
                outbox.getProcessingStartedAt(),
                outbox.getNextAttemptAt(),
                outbox.getSentAt(),
                outbox.getFailedAt(),
                outbox.getFailureReason(),
                outbox.getUpdatedAt()
        );
        return updated == 1;
    }

    private static void validate(NotificationOutbox outbox, LocalDateTime claimedProcessingStartedAt) {
        validateOutbox(outbox);
        validateClaimedProcessingStartedAt(claimedProcessingStartedAt);
    }

    private static void validateOutbox(NotificationOutbox outbox) {
        if (outbox == null) {
            throw new IllegalArgumentException("저장하려는 outbox 는 비어 있을 수 없습니다.");
        }
        if (outbox.getId() == null) {
            throw new IllegalArgumentException("저장하려는 outbox 의 id 는 비어 있을 수 없습니다.");
        }
    }

    private static void validateClaimedProcessingStartedAt(LocalDateTime claimedProcessingStartedAt) {
        if (claimedProcessingStartedAt == null) {
            throw new IllegalArgumentException("claim 시각은 비어 있을 수 없습니다.");
        }
    }
}
