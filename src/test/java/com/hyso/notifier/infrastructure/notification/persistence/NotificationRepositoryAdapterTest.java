package com.hyso.notifier.infrastructure.notification.persistence;

import com.hyso.notifier.application.IntegrationTest;
import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.NotificationType;
import com.hyso.notifier.domain.notification.repository.NotificationSaveResult;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@IntegrationTest
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationRepositoryAdapterTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 5, 9, 10, 0);

    @Autowired
    NotificationRepositoryAdapter notificationRepositoryAdapter;

    @Test
    void 알림을_저장한다() {
        Notification notification = notification(idempotencyKey("a"));

        Notification actual = notificationRepositoryAdapter.save(notification);

        assertAll(
                () -> assertThat(actual.getId()).isNotNull(),
                () -> assertThat(actual.getReceiverId()).isEqualTo(1L),
                () -> assertThat(actual.getType()).isEqualTo(NotificationType.ENROLLMENT_COMPLETED),
                () -> assertThat(actual.getChannel()).isEqualTo(NotificationChannel.EMAIL),
                () -> assertThat(actual.getRefType()).isEqualTo("ENROLLMENT"),
                () -> assertThat(actual.getRefId()).isEqualTo(100L),
                () -> assertThat(actual.getBody()).isEqualTo("수강 신청이 완료되었습니다."),
                () -> assertThat(actual.getIdempotencyKey()).isEqualTo(idempotencyKey("a")),
                () -> assertThat(actual.getCreatedAt()).isEqualTo(CREATED_AT)
        );
    }

    @Sql("/sql/notification/insert_notification.sql")
    @Test
    void 멱등성_키로_알림을_조회한다() {
        Optional<Notification> actual = notificationRepositoryAdapter.findByIdempotencyKey(
                "c43f82d4a0f6c91a5b2e7d8f9340ab1e6c5d2f8a7b9e0134d6a2c8f5e1b9073d"
        );

        assertThat(actual).isPresent();
        assertThat(actual.get().getId()).isEqualTo(1L);
    }

    @Test
    void 존재하지_않는_멱등성_키로_조회하면_빈_Optional을_반환한다() {
        Optional<Notification> actual = notificationRepositoryAdapter.findByIdempotencyKey(idempotencyKey("c"));

        assertThat(actual).isEmpty();
    }

    @Test
    void saveOrFind는_새_알림을_저장하고_created_결과를_반환한다() {
        String idempotencyKey = idempotencyKey("a");

        NotificationSaveResult actual = notificationRepositoryAdapter.saveOrFind(notification(idempotencyKey));

        assertAll(
                () -> assertThat(actual.created()).isTrue(),
                () -> assertThat(actual.notification().getId()).isNotNull(),
                () -> assertThat(actual.notification().getIdempotencyKey()).isEqualTo(idempotencyKey),
                () -> assertThat(notificationRepositoryAdapter.findByIdempotencyKey(idempotencyKey)).isPresent()
        );
    }

    @Sql("/sql/notification/insert_notification.sql")
    @Test
    void saveOrFind는_중복_알림이면_기존_알림과_existing_결과를_반환한다() {
        String idempotencyKey = "c43f82d4a0f6c91a5b2e7d8f9340ab1e6c5d2f8a7b9e0134d6a2c8f5e1b9073d";

        NotificationSaveResult actual = notificationRepositoryAdapter.saveOrFind(notification(idempotencyKey));

        assertAll(
                () -> assertThat(actual.created()).isFalse(),
                () -> assertThat(actual.notification().getId()).isEqualTo(1L),
                () -> assertThat(actual.notification().getIdempotencyKey()).isEqualTo(idempotencyKey)
        );
    }

    @Sql("/sql/notification/insert_notification.sql")
    @Test
    void markSent_는_알림의_sent_at_을_갱신한다() {
        LocalDateTime sentAt = LocalDateTime.of(2026, 5, 11, 12, 0);

        notificationRepositoryAdapter.markSent(1L, sentAt);

        Notification updated = notificationRepositoryAdapter.findByIdempotencyKey(
                "c43f82d4a0f6c91a5b2e7d8f9340ab1e6c5d2f8a7b9e0134d6a2c8f5e1b9073d"
        ).orElseThrow();
        assertAll(
                () -> assertThat(updated.getSentAt()).isEqualTo(sentAt),
                () -> assertThat(updated.getFailedAt()).isNull(),
                () -> assertThat(updated.getFailureReason()).isNull()
        );
    }

    @Sql("/sql/notification/insert_notification.sql")
    @Test
    void markFailed_는_알림의_failed_at_과_failure_reason_을_갱신한다() {
        LocalDateTime failedAt = LocalDateTime.of(2026, 5, 11, 12, 0);
        String reason = "lease timeout";

        notificationRepositoryAdapter.markFailed(1L, failedAt, reason);

        Notification updated = notificationRepositoryAdapter.findByIdempotencyKey(
                "c43f82d4a0f6c91a5b2e7d8f9340ab1e6c5d2f8a7b9e0134d6a2c8f5e1b9073d"
        ).orElseThrow();
        assertAll(
                () -> assertThat(updated.getFailedAt()).isEqualTo(failedAt),
                () -> assertThat(updated.getFailureReason()).isEqualTo(reason),
                () -> assertThat(updated.getSentAt()).isNull()
        );
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
