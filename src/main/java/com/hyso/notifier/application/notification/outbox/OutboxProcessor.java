package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.application.notification.outbox.dispatcher.NotificationDispatcherRegistry;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import com.hyso.notifier.global.config.OutboxRetryProperties;
import com.hyso.notifier.global.config.OutboxWorkerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final OutboxResultPersister outboxResultPersister;
    private final NotificationDispatcherRegistry notificationDispatcherRegistry;
    private final RetryExceptionClassifier retryExceptionClassifier;
    private final RetryBackoffCalculator retryBackoffCalculator;
    private final OutboxWorkerProperties outboxWorkerProperties;
    private final OutboxRetryProperties outboxRetryProperties;
    private final Clock clock;

    @Transactional
    public List<ClaimedOutbox> claimBatch() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<NotificationOutbox> outboxes = notificationOutboxRepository.findClaimableForUpdate(
                now,
                outboxWorkerProperties.batchSize()
        );
        for (NotificationOutbox outbox : outboxes) {
            outbox.claim(now);
        }
        return outboxes.stream()
                .map(outbox -> new ClaimedOutbox(outbox, outbox.getProcessingStartedAt()))
                .toList();
    }

    public void dispatchAndPersist(ClaimedOutbox claimed) {
        if (claimed == null) {
            throw new IllegalArgumentException("ClaimedOutbox 는 비어 있을 수 없습니다.");
        }
        NotificationOutbox outbox = claimed.outbox();
        boolean dispatched = tryDispatch(outbox);
        if (dispatched) {
            outbox.markDispatched(LocalDateTime.now(clock));
        }
        boolean persisted = outboxResultPersister.persist(outbox, claimed.claimedProcessingStartedAt());
        if (!persisted) {
            log.warn("outbox {} 의 lease 가 유실되어 결과 저장이 거부되었습니다", outbox.getId());
        }
    }

    private boolean tryDispatch(NotificationOutbox outbox) {
        try {
            notificationDispatcherRegistry.dispatch(outbox.getChannel(), outbox);
            return true;
        } catch (Exception cause) {
            handleFailure(outbox, cause);
            return false;
        }
    }

    private void handleFailure(NotificationOutbox outbox, Throwable cause) {
        LocalDateTime now = LocalDateTime.now(clock);
        RetryExceptionClassifier.Classification classification = retryExceptionClassifier.classify(cause);

        if (classification == RetryExceptionClassifier.Classification.NON_RETRYABLE) {
            outbox.markFailed(now, FailureReasons.fromException(cause));
            return;
        }
        if (outbox.getProcessingAttempt() >= outboxRetryProperties.maxAttempts()) {
            outbox.markFailed(now, FailureReasons.maxAttemptsExceeded(outbox.getProcessingAttempt(), cause));
            return;
        }
        Duration delay = retryBackoffCalculator.nextDelay(outbox.getProcessingAttempt());
        outbox.markRetryPending(now, FailureReasons.fromException(cause), now.plus(delay));
    }
}
