package com.hyso.notifier.infrastructure.notification.exception;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(Long notificationId) {
        super("알림 조회 실패: " + notificationId);
    }
}
