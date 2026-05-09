package com.hyso.notifier.domain.notification.outbox;

public enum NotificationOutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    RETRY_PENDING,
    FAILED
}
