package com.hyso.notifier.infrastructure.notification.persistence;

import com.hyso.notifier.application.IntegrationTest;
import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.NotificationType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@IntegrationTest
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationRepositoryAdapterTest {

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
                () -> assertThat(actual.getIdempotencyKey()).isEqualTo(idempotencyKey("a"))
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

    private Notification notification(String idempotencyKey) {
        return Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L,
                "수강 신청이 완료되었습니다.",
                idempotencyKey
        );
    }

    private String idempotencyKey(String seed) {
        return seed.repeat(64);
    }
}
