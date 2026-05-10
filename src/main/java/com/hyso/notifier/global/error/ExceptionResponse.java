package com.hyso.notifier.global.error;

public record ExceptionResponse(
        String errorCode,
        String message
) {

    public static ExceptionResponse from(ErrorCode errorCode) {
        return new ExceptionResponse(errorCode.getErrorCode(), errorCode.getMessage());
    }

    public static ExceptionResponse from(ErrorCode errorCode, String message) {
        return new ExceptionResponse(errorCode.getErrorCode(), message);
    }
}
