package com.hyso.notifier.presentation.notification.dto.request;

import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(

        @NotNull(message = "수신자 ID 는 비어 있을 수 없습니다.")
        Long receiverId,

        @NotNull(message = "알림 타입은 비어 있을 수 없습니다.")
        NotificationType type,

        @NotNull(message = "발송 채널은 비어 있을 수 없습니다.")
        NotificationChannel channel,

        @NotBlank(message = "참조 타입은 비어 있을 수 없습니다.")
        @Size(max = 64, message = "참조 타입은 64자를 넘을 수 없습니다.")
        String refType,

        @NotNull(message = "참조 ID 는 비어 있을 수 없습니다.")
        Long refId
) {
}
