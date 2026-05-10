package com.hyso.notifier.application.notification.outbox.exception;

public class TransientFailureException extends RuntimeException {

    public TransientFailureException(String message) {
        super(message);
    }

    public TransientFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
