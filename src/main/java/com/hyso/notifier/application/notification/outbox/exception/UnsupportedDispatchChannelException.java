package com.hyso.notifier.application.notification.outbox.exception;

import com.hyso.notifier.domain.notification.NotificationChannel;

public class UnsupportedDispatchChannelException extends RuntimeException {

    public UnsupportedDispatchChannelException(NotificationChannel channel) {
        super("등록되지 않은 발송 채널입니다: " + channel);
    }
}
