package com.hyso.notifier.application.notification.outbox;

import java.time.LocalDateTime;

public final class FailureReasons {

    private static final int MAX_LENGTH = 500;

    private FailureReasons() {
    }

    public static String fromException(Throwable cause) {
        validateCause(cause);
        String typeName = cause.getClass().getSimpleName();
        String message = cause.getMessage();
        String combined;
        if (message == null || message.isBlank()) {
            combined = typeName;
        } else {
            combined = typeName + ": " + message;
        }
        return truncate(combined);
    }

    public static String maxAttemptsExceeded(int attempts, Throwable cause) {
        validateAttempts(attempts);
        validateCause(cause);
        return truncate("max attempts exceeded (" + attempts + "): " + fromException(cause));
    }

    public static String leaseTimeout(LocalDateTime claimedProcessingStartedAt) {
        validateClaimedProcessingStartedAt(claimedProcessingStartedAt);
        return "lease timeout (claimed at " + claimedProcessingStartedAt + ")";
    }

    private static String truncate(String reason) {
        if (reason.length() <= MAX_LENGTH) {
            return reason;
        }
        return reason.substring(0, MAX_LENGTH);
    }

    private static void validateCause(Throwable cause) {
        if (cause == null) {
            throw new IllegalArgumentException("예외 원인은 비어 있을 수 없습니다.");
        }
    }

    private static void validateAttempts(int attempts) {
        if (attempts < 1) {
            throw new IllegalArgumentException("시도 횟수는 1 이상이어야 합니다.");
        }
    }

    private static void validateClaimedProcessingStartedAt(LocalDateTime claimedProcessingStartedAt) {
        if (claimedProcessingStartedAt == null) {
            throw new IllegalArgumentException("claim 시각은 비어 있을 수 없습니다.");
        }
    }
}
