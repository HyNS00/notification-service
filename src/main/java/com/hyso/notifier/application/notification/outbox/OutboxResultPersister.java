package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxStatus;
import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import com.hyso.notifier.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OutboxResultPersister {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationRepository notificationRepository;

    @Transactional
    public boolean persist(NotificationOutbox outbox, LocalDateTime claimedProcessingStartedAt) {
        validate(outbox, claimedProcessingStartedAt);
        boolean savedOutbox = notificationOutboxRepository.saveIfLeaseMatched(outbox, claimedProcessingStartedAt);
        if (!savedOutbox) {
            return false;
        }
        applyToNotification(outbox);
        return true;
    }

    private void applyToNotification(NotificationOutbox outbox) {
        NotificationOutboxStatus status = outbox.getStatus();
        if (status == NotificationOutboxStatus.SENT) {
            notificationRepository.markSent(outbox.getNotificationId(), outbox.getSentAt());
            return;
        }
        if (status == NotificationOutboxStatus.FAILED) {
            notificationRepository.markFailed(
                    outbox.getNotificationId(),
                    outbox.getFailedAt(),
                    outbox.getFailureReason()
            );
        }
    }

    private static void validate(NotificationOutbox outbox, LocalDateTime claimedProcessingStartedAt) {
        validateOutbox(outbox);
        validateClaimedProcessingStartedAt(claimedProcessingStartedAt);
    }

    private static void validateOutbox(NotificationOutbox outbox) {
        if (outbox == null) {
            throw new IllegalArgumentException("outbox 는 비어 있을 수 없습니다.");
        }
    }

    private static void validateClaimedProcessingStartedAt(LocalDateTime claimedProcessingStartedAt) {
        if (claimedProcessingStartedAt == null) {
            throw new IllegalArgumentException("claim 시각은 비어 있을 수 없습니다.");
        }
    }
}
