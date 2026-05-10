package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;

import java.time.LocalDateTime;

public final class OutboxFixtures {

    private static final String SAMPLE_IDEMPOTENCY_KEY =
            "c43f82d4a0f6c91a5b2e7d8f9340ab1e6c5d2f8a7b9e0134d6a2c8f5e1b9073d";
    private static final long SAMPLE_RECEIVER_ID = 42L;
    private static final NotificationChannel SAMPLE_CHANNEL = NotificationChannel.EMAIL;
    private static final String SAMPLE_BODY = "본문";

    private OutboxFixtures() {
    }

    public static NotificationOutbox pending(long notificationId, LocalDateTime createdAt) {
        return NotificationOutbox.create(
                notificationId,
                SAMPLE_IDEMPOTENCY_KEY,
                SAMPLE_RECEIVER_ID,
                SAMPLE_CHANNEL,
                SAMPLE_BODY,
                createdAt
        );
    }

    public static NotificationOutbox processingAtAttempt(
            long notificationId,
            LocalDateTime createdAt,
            LocalDateTime finalClaimedAt,
            int targetAttempt
    ) {
        NotificationOutbox outbox = pending(notificationId, createdAt);
        for (int i = 1; i < targetAttempt; i++) {
            outbox.claim(finalClaimedAt.minusMinutes(targetAttempt - i + 1L));
            outbox.markRetryPending(
                    finalClaimedAt.minusMinutes(targetAttempt - i),
                    "이전 실패",
                    finalClaimedAt.minusMinutes(targetAttempt - i - 1L)
            );
        }
        outbox.claim(finalClaimedAt);
        return outbox;
    }
}
