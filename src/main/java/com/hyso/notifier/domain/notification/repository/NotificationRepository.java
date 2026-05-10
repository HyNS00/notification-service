package com.hyso.notifier.domain.notification.repository;

import com.hyso.notifier.domain.notification.Notification;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification notification);

    NotificationSaveResult saveOrFind(Notification notification);

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    void markSent(Long notificationId, LocalDateTime sentAt);

    void markFailed(Long notificationId, LocalDateTime failedAt, String failureReason);
}
