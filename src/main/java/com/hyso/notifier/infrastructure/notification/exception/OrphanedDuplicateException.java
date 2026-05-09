package com.hyso.notifier.infrastructure.notification.exception;

public class OrphanedDuplicateException extends IllegalStateException {

    public OrphanedDuplicateException(String idempotencyKey) {
        super("기존 알림 조회 실패: " + idempotencyKey);
    }
}
