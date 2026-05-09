package com.hyso.notifier.domain.notification;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationTest {

    @Test
    void 알림을_생성한다() {
        Notification actual = Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L,
                "수강 신청이 완료되었습니다.",
                idempotencyKey()
        );

        assertAll(
                () -> assertThat(actual.getReceiverId()).isEqualTo(1L),
                () -> assertThat(actual.getType()).isEqualTo(NotificationType.ENROLLMENT_COMPLETED),
                () -> assertThat(actual.getChannel()).isEqualTo(NotificationChannel.EMAIL),
                () -> assertThat(actual.getRefType()).isEqualTo("ENROLLMENT"),
                () -> assertThat(actual.getRefId()).isEqualTo(100L),
                () -> assertThat(actual.getBody()).isEqualTo("수강 신청이 완료되었습니다."),
                () -> assertThat(actual.getIdempotencyKey()).isEqualTo(idempotencyKey()),
                () -> assertThat(actual.getSentAt()).isNull(),
                () -> assertThat(actual.getFailedAt()).isNull(),
                () -> assertThat(actual.getFailureReason()).isNull(),
                () -> assertThat(actual.getReadAt()).isNull()
        );
    }

    @Test
    void receiverId가_비어있으면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                null,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L,
                "수강 신청이 완료되었습니다.",
                idempotencyKey()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("수신자 ID 는 비어 있을 수 없습니다.");
    }

    @Test
    void type이_비어있으면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                1L,
                null,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L,
                "수강 신청이 완료되었습니다.",
                idempotencyKey()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 타입은 비어 있을 수 없습니다.");
    }

    @Test
    void channel이_비어있으면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                null,
                "ENROLLMENT",
                100L,
                "수강 신청이 완료되었습니다.",
                idempotencyKey()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("발송 채널은 비어 있을 수 없습니다.");
    }

    @Test
    void refType이_null이면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                null,
                100L,
                "수강 신청이 완료되었습니다.",
                idempotencyKey()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참조 타입은 비어 있을 수 없습니다.");
    }

    @Test
    void refType이_공백이면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                " ",
                100L,
                "수강 신청이 완료되었습니다.",
                idempotencyKey()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참조 타입은 비어 있을 수 없습니다.");
    }

    @Test
    void refType이_64자를_넘으면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "A".repeat(65),
                100L,
                "수강 신청이 완료되었습니다.",
                idempotencyKey()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참조 타입은 64자를 넘을 수 없습니다.");
    }

    @Test
    void refId가_비어있으면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                null,
                "수강 신청이 완료되었습니다.",
                idempotencyKey()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("참조 ID 는 비어 있을 수 없습니다.");
    }

    @Test
    void body가_null이면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L,
                null,
                idempotencyKey()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 본문은 비어 있을 수 없습니다.");
    }

    @Test
    void body가_공백이면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L,
                " ",
                idempotencyKey()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 본문은 비어 있을 수 없습니다.");
    }

    @Test
    void body가_500자를_넘으면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L,
                "A".repeat(501),
                idempotencyKey()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 본문은 500자를 넘을 수 없습니다.");
    }

    @Test
    void idempotencyKey가_null이면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L,
                "수강 신청이 완료되었습니다.",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("멱등성 키는 비어 있을 수 없습니다.");
    }

    @Test
    void idempotencyKey가_공백이면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L,
                "수강 신청이 완료되었습니다.",
                " "
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("멱등성 키는 비어 있을 수 없습니다.");
    }

    @Test
    void idempotencyKey가_64자를_넘으면_알림을_생성할_수_없다() {
        assertThatThrownBy(() -> Notification.create(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L,
                "수강 신청이 완료되었습니다.",
                "A".repeat(65)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("멱등성 키는 64자를 넘을 수 없습니다.");
    }

    private String idempotencyKey() {
        return "a".repeat(64);
    }
}
