package com.hyso.notifier.domain.notification.outbox;

public enum NotificationOutboxStatus {
    PENDING,
    PROCESSING,
    DISPATCHED,
    RETRY_PENDING,
    FAILED
}
