package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import com.hyso.notifier.domain.notification.repository.NotificationRepository;
import com.hyso.notifier.global.config.OutboxRecoveryProperties;
import com.hyso.notifier.global.config.OutboxRetryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxTimeoutRecoveryWorker {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationRepository notificationRepository;
    private final OutboxRecoveryProperties outboxRecoveryProperties;
    private final OutboxRetryProperties outboxRetryProperties;
    private final RetryBackoffCalculator retryBackoffCalculator;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${outbox.recovery.fixed-delay-ms}")
    @Transactional
    public void recoverStuckRows() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime cutoff = now.minus(Duration.ofMillis(outboxRecoveryProperties.leaseTimeoutMs()));

        List<NotificationOutbox> stuck = notificationOutboxRepository.findRecoverableForUpdate(
                cutoff,
                outboxRecoveryProperties.batchSize()
        );

        if (stuck.isEmpty()) {
            return;
        }

        log.info("OutboxTimeoutRecoveryWorker — 회복 대상 {} 행 발견", stuck.size());
        for (NotificationOutbox outbox : stuck) {
            recoverOne(outbox, now);
        }
    }

    private void recoverOne(NotificationOutbox outbox, LocalDateTime now) {
        LocalDateTime claimedProcessingStartedAt = outbox.getProcessingStartedAt();
        String reason = FailureReasons.leaseTimeout(claimedProcessingStartedAt);

        if (outbox.getProcessingAttempt() >= outboxRetryProperties.maxAttempts()) {
            outbox.markFailed(now, reason);
            notificationRepository.markFailed(outbox.getNotificationId(), now, reason);
            return;
        }
        Duration delay = retryBackoffCalculator.nextDelay(outbox.getProcessingAttempt());
        outbox.markRetryPending(now, reason, now.plus(delay));
    }
}
