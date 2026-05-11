package com.hyso.notifier.global.error;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    ORPHANED_DUPLICATE("ORPHANED_DUPLICATE", "알림 등록 처리에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    UNSUPPORTED_DISPATCH_CHANNEL("UNSUPPORTED_DISPATCH_CHANNEL", "발송 채널을 처리할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    NOTIFICATION_NOT_FOUND("NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_CHANNEL_FOR_READ("INVALID_CHANNEL_FOR_READ", "이 채널의 알림은 읽음 처리할 수 없습니다.", HttpStatus.BAD_REQUEST);

    private final String errorCode;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
