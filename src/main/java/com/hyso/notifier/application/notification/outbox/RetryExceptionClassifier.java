package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.application.notification.outbox.exception.TransientFailureException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RetryExceptionClassifier {

    public Classification classify(Throwable cause) {
        validate(cause);
        if (isRetryable(cause)) {
            return Classification.RETRYABLE;
        }
        return Classification.NON_RETRYABLE;
    }

    private boolean isRetryable(Throwable cause) {
        return cause instanceof TransientFailureException
                || cause instanceof IOException
                || cause instanceof TimeoutException;
    }

    private static void validate(Throwable cause) {
        if (cause == null) {
            throw new IllegalArgumentException("예외 원인은 비어 있을 수 없습니다.");
        }
    }

    public enum Classification {
        RETRYABLE,
        NON_RETRYABLE
    }
}
