package com.hyso.notifier.domain.notification.repository;

import com.hyso.notifier.domain.notification.Notification;

public record NotificationSaveResult(
        Notification notification,
        boolean created
) {

    public static NotificationSaveResult created(Notification notification) {
        return new NotificationSaveResult(notification, true);
    }

    public static NotificationSaveResult existing(Notification notification) {
        return new NotificationSaveResult(notification, false);
    }
}
