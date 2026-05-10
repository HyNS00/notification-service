package com.hyso.notifier.infrastructure.notification.persistence;

import com.hyso.notifier.application.IntegrationTest;
import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.NotificationType;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxLeaseState;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxStatus;
import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import com.hyso.notifier.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@IntegrationTest
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationCreatorTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 5, 9, 10, 0);
    private static final String EXISTING_IDEMPOTENCY_KEY =
            "c43f82d4a0f6c91a5b2e7d8f9340ab1e6c5d2f8a7b9e0134d6a2c8f5e1b9073d";

    @Autowired
    NotificationCreator notificationCreator;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    NotificationOutboxRepository notificationOutboxRepository;

    @Test
    void 알림과_아웃박스를_같은_멱등성_키로_저장한다() {
        String idempotencyKey = idempotencyKey("a");

        Notification created = notificationCreator.saveNew(notification(idempotencyKey));

        Notification savedNotification = notificationRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        NotificationOutbox savedOutbox = notificationOutboxRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        assertAll(
                () -> assertThat(created.getId()).isEqualTo(savedNotification.getId()),
                () -> assertThat(savedOutbox.getNotificationId()).isEqualTo(savedNotification.getId()),
                () -> assertThat(savedOutbox.getIdempotencyKey()).isEqualTo(savedNotification.getIdempotencyKey()),
                () -> assertThat(savedOutbox.getReceiverId()).isEqualTo(savedNotification.getReceiverId()),
                () -> assertThat(savedOutbox.getChannel()).isEqualTo(savedNotification.getChannel()),
                () -> assertThat(savedOutbox.getBody()).isEqualTo(savedNotification.getBody()),
                () -> assertThat(savedOutbox.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING),
                () -> assertThat(savedOutbox.getProcessingAttempt()).isZero(),
                () -> assertThat(savedOutbox.getProcessingLeaseState()).isEqualTo(NotificationOutboxLeaseState.IDLE),
                () -> assertThat(savedOutbox.getProcessingStartedAt()).isNull(),
                () -> assertThat(savedOutbox.getCreatedAt()).isEqualTo(CREATED_AT),
                () -> assertThat(savedOutbox.getUpdatedAt()).isEqualTo(CREATED_AT)
        );
    }

    @Sql("/sql/notification/insert_notification.sql")
    @Test
    void 같은_멱등성_키로_saveNew를_호출하면_unique_예외가_발생한다() {
        assertThatThrownBy(() -> notificationCreator.saveNew(notification(EXISTING_IDEMPOTENCY_KEY)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Notification notification(String idempotencyKey) {
        return Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L,
                "수강 신청이 완료되었습니다.",
                idempotencyKey,
                CREATED_AT
        );
    }

    private String idempotencyKey(String seed) {
        return seed.repeat(64);
    }
}
