package com.hyso.notifier.application.notification;

import com.hyso.notifier.domain.notification.NotificationType;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageRenderer {

    public String render(NotificationType type) {
        validate(type);
        return switch (type) {
            case ENROLLMENT_COMPLETED -> "수강 신청이 완료되었습니다.";
            case PAYMENT_CONFIRMED -> "결제가 승인되었습니다.";
            case COURSE_START_D1 -> "내일 강의가 시작됩니다.";
            case ENROLLMENT_CANCELLED -> "수강 신청이 취소되었습니다.";
        };
    }

    private void validate(NotificationType type) {
        if (type == null) {
            throw new IllegalArgumentException("알림 타입은 비어 있을 수 없습니다.");
        }
    }
}
