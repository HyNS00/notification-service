package com.hyso.notifier.domain.notification.repository;

import com.hyso.notifier.domain.notification.Notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository {

    Notification save(Notification notification);

    NotificationSaveResult saveOrFind(Notification notification);

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);

    Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);

    List<Notification> findPage(Long receiverId, ReadFilter filter, int limit);

    void markSent(Long notificationId, LocalDateTime sentAt);

    void markFailed(Long notificationId, LocalDateTime failedAt, String failureReason);
}
