package com.hyso.notifier.domain.notification.outbox.repository;

import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;

import java.util.Optional;

public interface NotificationOutboxRepository {

    NotificationOutbox save(NotificationOutbox notificationOutbox);

    Optional<NotificationOutbox> findByIdempotencyKey(String idempotencyKey);
}
