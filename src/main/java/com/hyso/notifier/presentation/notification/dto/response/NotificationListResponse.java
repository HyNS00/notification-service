package com.hyso.notifier.presentation.notification.dto.response;

import com.hyso.notifier.domain.notification.Notification;

import java.util.List;

public record NotificationListResponse(List<NotificationResponse> items) {

    public static NotificationListResponse of(List<Notification> notifications) {
        return new NotificationListResponse(
                notifications.stream()
                        .map(notification -> NotificationResponse.from(notification))
                        .toList()
        );
    }
}
