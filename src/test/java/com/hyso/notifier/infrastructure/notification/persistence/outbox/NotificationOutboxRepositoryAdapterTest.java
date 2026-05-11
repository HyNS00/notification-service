package com.hyso.notifier.infrastructure.notification.persistence.outbox;

import com.hyso.notifier.application.IntegrationTest;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxLeaseState;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxStatus;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@IntegrationTest
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationOutboxRepositoryAdapterTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 5, 9, 10, 0);

    @Autowired
    NotificationOutboxRepositoryAdapter notificationOutboxRepositoryAdapter;

    @Test
    void 아웃박스를_저장한다() {
        NotificationOutbox outbox = outbox(10L, idempotencyKey("a"));

        NotificationOutbox actual = notificationOutboxRepositoryAdapter.save(outbox);

        assertAll(
                () -> assertThat(actual.getId()).isNotNull(),
                () -> assertThat(actual.getNotificationId()).isEqualTo(10L),
                () -> assertThat(actual.getIdempotencyKey()).isEqualTo(idempotencyKey("a")),
                () -> assertThat(actual.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING),
                () -> assertThat(actual.getProcessingAttempt()).isZero(),
                () -> assertThat(actual.getProcessingLeaseState()).isEqualTo(NotificationOutboxLeaseState.IDLE),
                () -> assertThat(actual.getProcessingStartedAt()).isNull(),
                () -> assertThat(actual.getReceiverId()).isEqualTo(1L),
                () -> assertThat(actual.getChannel()).isEqualTo(NotificationChannel.EMAIL),
                () -> assertThat(actual.getBody()).isEqualTo("수강 신청이 완료되었습니다."),
                () -> assertThat(actual.getFailureReason()).isNull(),
                () -> assertThat(actual.getDispatchedAt()).isNull(),
                () -> assertThat(actual.getFailedAt()).isNull(),
                () -> assertThat(actual.getCreatedAt()).isEqualTo(CREATED_AT),
                () -> assertThat(actual.getUpdatedAt()).isEqualTo(CREATED_AT)
        );
    }

    @Test
    void 멱등성_키로_아웃박스를_조회한다() {
        NotificationOutbox saved = notificationOutboxRepositoryAdapter.save(outbox(10L, idempotencyKey("a")));

        Optional<NotificationOutbox> actual = notificationOutboxRepositoryAdapter.findByIdempotencyKey(
                idempotencyKey("a")
        );

        assertThat(actual).isPresent();
        assertThat(actual.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void 존재하지_않는_멱등성_키로_조회하면_빈_Optional을_반환한다() {
        Optional<NotificationOutbox> actual = notificationOutboxRepositoryAdapter.findByIdempotencyKey(
                idempotencyKey("a")
        );

        assertThat(actual).isEmpty();
    }

    @Test
    void 같은_멱등성_키로_아웃박스를_저장할_수_없다() {
        notificationOutboxRepositoryAdapter.save(outbox(10L, idempotencyKey("a")));

        assertThatThrownBy(() -> notificationOutboxRepositoryAdapter.save(outbox(11L, idempotencyKey("a"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_알림_ID로_아웃박스를_저장할_수_없다() {
        notificationOutboxRepositoryAdapter.save(outbox(10L, idempotencyKey("a")));

        assertThatThrownBy(() -> notificationOutboxRepositoryAdapter.save(outbox(10L, idempotencyKey("b"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private NotificationOutbox outbox(Long notificationId, String idempotencyKey) {
        return NotificationOutbox.create(
                notificationId,
                idempotencyKey,
                1L,
                NotificationChannel.EMAIL,
                "수강 신청이 완료되었습니다.",
                CREATED_AT
        );
    }

    private String idempotencyKey(String seed) {
        return seed.repeat(64);
    }
}
