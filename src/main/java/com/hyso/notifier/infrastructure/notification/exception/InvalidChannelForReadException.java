package com.hyso.notifier.infrastructure.notification.exception;

import com.hyso.notifier.domain.notification.NotificationChannel;

public class InvalidChannelForReadException extends RuntimeException {

    public InvalidChannelForReadException(NotificationChannel channel) {
        super("읽음 처리할 수 없는 채널: " + channel);
    }
}
