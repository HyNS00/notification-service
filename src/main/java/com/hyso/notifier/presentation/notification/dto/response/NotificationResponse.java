package com.hyso.notifier.presentation.notification.dto.response;

import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        NotificationChannel channel,
        String refType,
        Long refId,
        String body,
        NotificationStatus status,
        LocalDateTime sentAt,
        LocalDateTime failedAt,
        String failureReason,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getChannel(),
                notification.getRefType(),
                notification.getRefId(),
                notification.getBody(),
                deriveStatus(notification),
                notification.getSentAt(),
                notification.getFailedAt(),
                notification.getFailureReason(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }

    private static NotificationStatus deriveStatus(Notification notification) {
        if (notification.getSentAt() != null) {
            return NotificationStatus.SENT;
        }
        if (notification.getFailedAt() != null) {
            return NotificationStatus.FAILED;
        }
        return NotificationStatus.PENDING;
    }
}
