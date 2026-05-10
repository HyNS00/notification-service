package com.hyso.notifier.application.notification;

import com.hyso.notifier.application.IntegrationTest;
import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.NotificationType;
import com.hyso.notifier.domain.notification.repository.NotificationRepository;
import com.hyso.notifier.presentation.notification.dto.request.CreateNotificationRequest;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@IntegrationTest
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationServiceIntegrationTest {

    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    IdempotencyKeyGenerator idempotencyKeyGenerator;

    @Test
    void 새_요청을_등록하면_요청_값과_렌더링된_본문으로_알림이_저장된다() {
        CreateNotificationRequest request = request();
        String idempotencyKey = idempotencyKey(request);

        RegisterNotificationResult result = notificationService.register(request);

        Notification saved = notificationRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        assertAll(
                () -> assertThat(result.id()).isEqualTo(saved.getId()),
                () -> assertThat(result.created()).isTrue(),
                () -> assertThat(saved.getReceiverId()).isEqualTo(request.receiverId()),
                () -> assertThat(saved.getType()).isEqualTo(request.type()),
                () -> assertThat(saved.getChannel()).isEqualTo(request.channel()),
                () -> assertThat(saved.getRefType()).isEqualTo(request.refType()),
                () -> assertThat(saved.getRefId()).isEqualTo(request.refId()),
                () -> assertThat(saved.getBody()).isEqualTo("수강 신청이 완료되었습니다."),
                () -> assertThat(saved.getIdempotencyKey()).isEqualTo(idempotencyKey)
        );
    }

    @Test
    void 같은_요청을_다시_등록하면_기존_id를_그대로_반환한다() {
        CreateNotificationRequest request = request();

        RegisterNotificationResult first = notificationService.register(request);
        RegisterNotificationResult second = notificationService.register(request);

        assertAll(
                () -> assertThat(second.id()).isEqualTo(first.id()),
                () -> assertThat(second.created()).isFalse()
        );
    }

    private CreateNotificationRequest request() {
        return new CreateNotificationRequest(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L
        );
    }

    private String idempotencyKey(CreateNotificationRequest request) {
        return idempotencyKeyGenerator.generate(
                request.receiverId(),
                request.type(),
                request.refType(),
                request.refId(),
                request.channel()
        );
    }
}
