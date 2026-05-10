package com.hyso.notifier.application.notification;

public record RegisterNotificationResult(
        Long id,
        boolean created
) {

    public static RegisterNotificationResult created(Long id) {
        return new RegisterNotificationResult(id, true);
    }

    public static RegisterNotificationResult existing(Long id) {
        return new RegisterNotificationResult(id, false);
    }
}
