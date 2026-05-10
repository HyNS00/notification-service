package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;

import java.time.LocalDateTime;

public record ClaimedOutbox(NotificationOutbox outbox, LocalDateTime claimedProcessingStartedAt) {

    public ClaimedOutbox {
        if (outbox == null) {
            throw new IllegalArgumentException("ClaimedOutbox 의 outbox 는 비어 있을 수 없습니다.");
        }
        if (claimedProcessingStartedAt == null) {
            throw new IllegalArgumentException("ClaimedOutbox 의 claim 시각은 비어 있을 수 없습니다.");
        }
    }
}
