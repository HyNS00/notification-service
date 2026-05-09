package com.hyso.notifier.domain.notification.outbox;

import com.hyso.notifier.domain.notification.NotificationChannel;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationOutboxTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 5, 9, 10, 0);

    @Test
    void 아웃박스를_생성한다() {
        NotificationOutbox actual = createOutbox(
                10L,
                idempotencyKey(),
                1L,
                NotificationChannel.EMAIL,
                "수강 신청이 완료되었습니다."
        );

        assertAll(
                () -> assertThat(actual.getNotificationId()).isEqualTo(10L),
                () -> assertThat(actual.getIdempotencyKey()).isEqualTo(idempotencyKey()),
                () -> assertThat(actual.getReceiverId()).isEqualTo(1L),
                () -> assertThat(actual.getChannel()).isEqualTo(NotificationChannel.EMAIL),
                () -> assertThat(actual.getBody()).isEqualTo("수강 신청이 완료되었습니다."),
                () -> assertThat(actual.getStatus()).isEqualTo(NotificationOutboxStatus.PENDING),
                () -> assertThat(actual.getProcessingAttempt()).isZero(),
                () -> assertThat(actual.getProcessingLeaseState()).isEqualTo(NotificationOutboxLeaseState.IDLE),
                () -> assertThat(actual.getProcessingStartedAt()).isNull(),
                () -> assertThat(actual.getFailureReason()).isNull(),
                () -> assertThat(actual.getSentAt()).isNull(),
                () -> assertThat(actual.getFailedAt()).isNull(),
                () -> assertThat(actual.getCreatedAt()).isEqualTo(CREATED_AT),
                () -> assertThat(actual.getUpdatedAt()).isEqualTo(CREATED_AT)
        );
    }

    @Test
    void notificationId가_비어있으면_아웃박스를_생성할_수_없다() {
        assertThatThrownBy(() -> createOutbox(
                null,
                idempotencyKey(),
                1L,
                NotificationChannel.EMAIL,
                "수강 신청이 완료되었습니다."
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 ID 는 비어 있을 수 없습니다.");
    }

    @Test
    void idempotencyKey가_null이면_아웃박스를_생성할_수_없다() {
        assertThatThrownBy(() -> createOutbox(
                10L,
                null,
                1L,
                NotificationChannel.EMAIL,
                "수강 신청이 완료되었습니다."
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("멱등성 키는 비어 있을 수 없습니다.");
    }

    @Test
    void idempotencyKey가_공백이면_아웃박스를_생성할_수_없다() {
        assertThatThrownBy(() -> createOutbox(
                10L,
                " ",
                1L,
                NotificationChannel.EMAIL,
                "수강 신청이 완료되었습니다."
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("멱등성 키는 비어 있을 수 없습니다.");
    }

    @Test
    void idempotencyKey가_64자를_넘으면_아웃박스를_생성할_수_없다() {
        assertThatThrownBy(() -> createOutbox(
                10L,
                "A".repeat(65),
                1L,
                NotificationChannel.EMAIL,
                "수강 신청이 완료되었습니다."
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("멱등성 키는 64자를 넘을 수 없습니다.");
    }

    @Test
    void receiverId가_비어있으면_아웃박스를_생성할_수_없다() {
        assertThatThrownBy(() -> createOutbox(
                10L,
                idempotencyKey(),
                null,
                NotificationChannel.EMAIL,
                "수강 신청이 완료되었습니다."
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수신자 ID 는 비어 있을 수 없습니다.");
    }

    @Test
    void channel이_비어있으면_아웃박스를_생성할_수_없다() {
        assertThatThrownBy(() -> createOutbox(
                10L,
                idempotencyKey(),
                1L,
                null,
                "수강 신청이 완료되었습니다."
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("발송 채널은 비어 있을 수 없습니다.");
    }

    @Test
    void body가_null이면_아웃박스를_생성할_수_없다() {
        assertThatThrownBy(() -> createOutbox(
                10L,
                idempotencyKey(),
                1L,
                NotificationChannel.EMAIL,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 본문은 비어 있을 수 없습니다.");
    }

    @Test
    void body가_공백이면_아웃박스를_생성할_수_없다() {
        assertThatThrownBy(() -> createOutbox(
                10L,
                idempotencyKey(),
                1L,
                NotificationChannel.EMAIL,
                " "
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 본문은 비어 있을 수 없습니다.");
    }

    @Test
    void body가_500자를_넘으면_아웃박스를_생성할_수_없다() {
        assertThatThrownBy(() -> createOutbox(
                10L,
                idempotencyKey(),
                1L,
                NotificationChannel.EMAIL,
                "A".repeat(501)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 본문은 500자를 넘을 수 없습니다.");
    }

    @Test
    void createdAt이_비어있으면_아웃박스를_생성할_수_없다() {
        assertThatThrownBy(() -> NotificationOutbox.create(
                10L,
                idempotencyKey(),
                1L,
                NotificationChannel.EMAIL,
                "수강 신청이 완료되었습니다.",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("생성 시각은 비어 있을 수 없습니다.");
    }

    private NotificationOutbox createOutbox(
            Long notificationId,
            String idempotencyKey,
            Long receiverId,
            NotificationChannel channel,
            String body
    ) {
        return NotificationOutbox.create(notificationId, idempotencyKey, receiverId, channel, body, CREATED_AT);
    }

    private String idempotencyKey() {
        return "a".repeat(64);
    }
}
