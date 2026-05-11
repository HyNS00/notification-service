package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import com.hyso.notifier.global.config.OutboxCleanupProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxCleanupScheduler {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final OutboxCleanupProperties outboxCleanupProperties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${outbox.cleanup.fixed-delay-ms}")
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now(clock);
        int batchSize = outboxCleanupProperties.batchSize();

        LocalDateTime dispatchedCutoff = now.minusDays(outboxCleanupProperties.dispatchedRetentionDays());
        int dispatchedDeleted = notificationOutboxRepository.deleteDispatchedOlderThan(dispatchedCutoff, batchSize);

        LocalDateTime failedCutoff = now.minusDays(outboxCleanupProperties.failedRetentionDays());
        int failedDeleted = notificationOutboxRepository.deleteFailedOlderThan(failedCutoff, batchSize);

        if (dispatchedDeleted > 0 || failedDeleted > 0) {
            log.info("OutboxCleanup — DISPATCHED {} 행, FAILED {} 행 삭제", dispatchedDeleted, failedDeleted);
        }
    }
}
