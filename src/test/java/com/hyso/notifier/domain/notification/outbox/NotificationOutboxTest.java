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
                () -> assertThat(actual.getDispatchedAt()).isNull(),
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

    @Test
    void claim_은_PENDING_을_PROCESSING_으로_바꾸고_attempt_를_1_증가시킨다() {
        NotificationOutbox outbox = pendingOutbox();
        LocalDateTime claimAt = LocalDateTime.of(2026, 5, 10, 12, 0);

        outbox.claim(claimAt);

        assertAll(
                () -> assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PROCESSING),
                () -> assertThat(outbox.getProcessingLeaseState()).isEqualTo(NotificationOutboxLeaseState.CLAIMED),
                () -> assertThat(outbox.getProcessingStartedAt()).isEqualTo(claimAt),
                () -> assertThat(outbox.getProcessingAttempt()).isEqualTo(1),
                () -> assertThat(outbox.getNextAttemptAt()).isNull(),
                () -> assertThat(outbox.getUpdatedAt()).isEqualTo(claimAt)
        );
    }

    @Test
    void claim_은_RETRY_PENDING_에서도_attempt_를_누적해서_PROCESSING_으로_바꾼다() {
        NotificationOutbox outbox = pendingOutbox();
        LocalDateTime firstClaim = LocalDateTime.of(2026, 5, 10, 12, 0);
        LocalDateTime failed = firstClaim.plusSeconds(5);
        LocalDateTime nextAttempt = failed.plusSeconds(10);
        outbox.claim(firstClaim);
        outbox.markRetryPending(failed, "일시 장애", nextAttempt);

        LocalDateTime secondClaim = nextAttempt.plusSeconds(1);
        outbox.claim(secondClaim);

        assertAll(
                () -> assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.PROCESSING),
                () -> assertThat(outbox.getProcessingAttempt()).isEqualTo(2),
                () -> assertThat(outbox.getProcessingStartedAt()).isEqualTo(secondClaim),
                () -> assertThat(outbox.getNextAttemptAt()).isNull()
        );
    }

    @Test
    void claim_은_PROCESSING_상태에서_사용할_수_없다() {
        NotificationOutbox outbox = pendingOutbox();
        LocalDateTime claimAt = LocalDateTime.of(2026, 5, 10, 12, 0);
        outbox.claim(claimAt);

        assertThatThrownBy(() -> outbox.claim(claimAt.plusSeconds(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("PENDING 또는 RETRY_PENDING 상태에서만 claim 할 수 있습니다.");
    }

    @Test
    void markDispatched_는_PROCESSING_을_DISPATCHED_로_바꾸고_lease_와_failure_정보를_정리한다() {
        NotificationOutbox outbox = pendingOutbox();
        LocalDateTime claimAt = LocalDateTime.of(2026, 5, 10, 12, 0);
        LocalDateTime dispatchedAt = claimAt.plusSeconds(3);
        outbox.claim(claimAt);

        outbox.markDispatched(dispatchedAt);

        assertAll(
                () -> assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.DISPATCHED),
                () -> assertThat(outbox.getProcessingLeaseState()).isEqualTo(NotificationOutboxLeaseState.IDLE),
                () -> assertThat(outbox.getProcessingStartedAt()).isNull(),
                () -> assertThat(outbox.getDispatchedAt()).isEqualTo(dispatchedAt),
                () -> assertThat(outbox.getFailedAt()).isNull(),
                () -> assertThat(outbox.getFailureReason()).isNull(),
                () -> assertThat(outbox.getNextAttemptAt()).isNull(),
                () -> assertThat(outbox.getUpdatedAt()).isEqualTo(dispatchedAt)
        );
    }

    @Test
    void markRetryPending_은_PROCESSING_을_RETRY_PENDING_으로_바꾸고_failure_reason_과_next_attempt_at_을_저장한다() {
        NotificationOutbox outbox = pendingOutbox();
        LocalDateTime claimAt = LocalDateTime.of(2026, 5, 10, 12, 0);
        LocalDateTime failedAt = claimAt.plusSeconds(5);
        LocalDateTime nextAttempt = failedAt.plusSeconds(10);
        outbox.claim(claimAt);

        outbox.markRetryPending(failedAt, "일시 장애", nextAttempt);

        assertAll(
                () -> assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.RETRY_PENDING),
                () -> assertThat(outbox.getProcessingLeaseState()).isEqualTo(NotificationOutboxLeaseState.IDLE),
                () -> assertThat(outbox.getProcessingStartedAt()).isNull(),
                () -> assertThat(outbox.getFailedAt()).isEqualTo(failedAt),
                () -> assertThat(outbox.getFailureReason()).isEqualTo("일시 장애"),
                () -> assertThat(outbox.getNextAttemptAt()).isEqualTo(nextAttempt),
                () -> assertThat(outbox.getUpdatedAt()).isEqualTo(failedAt)
        );
    }

    @Test
    void markFailed_는_PROCESSING_을_FAILED_로_바꾸고_failure_reason_을_저장한다() {
        NotificationOutbox outbox = pendingOutbox();
        LocalDateTime claimAt = LocalDateTime.of(2026, 5, 10, 12, 0);
        LocalDateTime failedAt = claimAt.plusSeconds(5);
        outbox.claim(claimAt);

        outbox.markFailed(failedAt, "max attempts exceeded");

        assertAll(
                () -> assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.FAILED),
                () -> assertThat(outbox.getProcessingLeaseState()).isEqualTo(NotificationOutboxLeaseState.IDLE),
                () -> assertThat(outbox.getProcessingStartedAt()).isNull(),
                () -> assertThat(outbox.getFailedAt()).isEqualTo(failedAt),
                () -> assertThat(outbox.getFailureReason()).isEqualTo("max attempts exceeded"),
                () -> assertThat(outbox.getNextAttemptAt()).isNull(),
                () -> assertThat(outbox.getUpdatedAt()).isEqualTo(failedAt)
        );
    }

    @Test
    void mark_메서드는_PROCESSING_이_아니면_사용할_수_없다() {
        NotificationOutbox pending = pendingOutbox();
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0);

        assertAll(
                () -> assertThatThrownBy(() -> pending.markDispatched(now))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("PROCESSING 상태에서만 결과를 기록할 수 있습니다."),
                () -> assertThatThrownBy(() -> pending.markRetryPending(now, "사유", now.plusSeconds(10)))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("PROCESSING 상태에서만 결과를 기록할 수 있습니다."),
                () -> assertThatThrownBy(() -> pending.markFailed(now, "사유"))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("PROCESSING 상태에서만 결과를 기록할 수 있습니다.")
        );
    }

    @Test
    void mark_메서드의_입력이_부적절하면_사용할_수_없다() {
        NotificationOutbox processing = pendingOutbox();
        LocalDateTime claimAt = LocalDateTime.of(2026, 5, 10, 12, 0);
        LocalDateTime failedAt = claimAt.plusSeconds(5);
        LocalDateTime nextAttempt = failedAt.plusSeconds(10);
        processing.claim(claimAt);

        assertAll(
                () -> assertThatThrownBy(() -> processing.markDispatched(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("발송 위임 완료 시각은 비어 있을 수 없습니다."),
                () -> assertThatThrownBy(() -> processing.markRetryPending(null, "사유", nextAttempt))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("실패 시각은 비어 있을 수 없습니다."),
                () -> assertThatThrownBy(() -> processing.markRetryPending(failedAt, " ", nextAttempt))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("실패 사유는 비어 있을 수 없습니다."),
                () -> assertThatThrownBy(() -> processing.markRetryPending(failedAt, "x".repeat(501), nextAttempt))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("실패 사유는 500자를 넘을 수 없습니다."),
                () -> assertThatThrownBy(() -> processing.markRetryPending(failedAt, "사유", null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("다음 시도 시각은 비어 있을 수 없습니다."),
                () -> assertThatThrownBy(() -> processing.markFailed(failedAt, null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("실패 사유는 비어 있을 수 없습니다.")
        );
    }

    @Test
    void claim_시각이_null_이면_사용할_수_없다() {
        NotificationOutbox outbox = pendingOutbox();

        assertThatThrownBy(() -> outbox.claim(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("claim 시각은 비어 있을 수 없습니다.");
    }

    private NotificationOutbox pendingOutbox() {
        return createOutbox(10L, idempotencyKey(), 1L, NotificationChannel.EMAIL, "수강 신청이 완료되었습니다.");
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
