package com.hyso.notifier.application.notification;

import com.hyso.notifier.application.IntegrationTest;
import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.NotificationType;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxLeaseState;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxStatus;
import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import com.hyso.notifier.domain.notification.repository.NotificationRepository;
import com.hyso.notifier.presentation.notification.dto.request.CreateNotificationRequest;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@IntegrationTest
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationServiceIntegrationTest {

    @Autowired
    NotificationService notificationService;

    @Autowired
    IdempotencyKeyGenerator idempotencyKeyGenerator;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void 새_알림을_등록하면_알림과_아웃박스를_저장하고_created_true를_반환한다() {
        CreateNotificationRequest request = request();
        String idempotencyKey = idempotencyKey(request);

        RegisterNotificationResult result = notificationService.register(request);

        Notification notification = notificationRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        NotificationOutbox outbox = notificationOutboxRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        assertAll(
                () -> assertThat(result).isEqualTo(RegisterNotificationResult.created(notification.getId())),
                () -> assertThat(notification.getReceiverId()).isEqualTo(request.receiverId()),
                () -> assertThat(notification.getType()).isEqualTo(request.type()),
                () -> assertThat(notification.getChannel()).isEqualTo(request.channel()),
                () -> assertThat(notification.getRefType()).isEqualTo(request.refType()),
                () -> assertThat(notification.getRefId()).isEqualTo(request.refId()),
                () -> assertThat(notification.getBody()).isEqualTo("수강 신청이 완료되었습니다."),
                () -> assertThat(notification.getCreatedAt()).isNotNull(),
                () -> assertThat(outbox.getNotificationId()).isEqualTo(notification.getId()),
                () -> assertThat(outbox.getIdempotencyKey()).isEqualTo(notification.getIdempotencyKey()),
                () -> assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING),
                () -> assertThat(outbox.getProcessingAttempt()).isZero(),
                () -> assertThat(outbox.getProcessingLeaseState()).isEqualTo(NotificationOutboxLeaseState.IDLE),
                () -> assertThat(outbox.getProcessingStartedAt()).isNull(),
                () -> assertThat(outbox.getCreatedAt()).isNotNull(),
                () -> assertThat(outbox.getUpdatedAt()).isEqualTo(outbox.getCreatedAt())
        );
    }

    @Test
    void 같은_알림을_다시_등록하면_기존_id와_created_false를_반환하고_행을_추가하지_않는다() {
        CreateNotificationRequest request = request();
        String idempotencyKey = idempotencyKey(request);

        RegisterNotificationResult first = notificationService.register(request);
        RegisterNotificationResult second = notificationService.register(request);

        assertAll(
                () -> assertThat(first.created()).isTrue(),
                () -> assertThat(second).isEqualTo(RegisterNotificationResult.existing(first.id())),
                () -> assertThat(countNotifications(idempotencyKey)).isEqualTo(1L),
                () -> assertThat(countOutboxes(idempotencyKey)).isEqualTo(1L)
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

    private Long countNotifications(String idempotencyKey) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE idempotency_key = ?",
                Long.class,
                idempotencyKey
        );
    }

    private Long countOutboxes(String idempotencyKey) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_outboxes WHERE idempotency_key = ?",
                Long.class,
                idempotencyKey
        );
    }
}
