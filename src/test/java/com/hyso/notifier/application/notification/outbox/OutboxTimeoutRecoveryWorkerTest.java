package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxStatus;
import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import com.hyso.notifier.domain.notification.repository.NotificationRepository;
import com.hyso.notifier.global.config.OutboxRecoveryProperties;
import com.hyso.notifier.global.config.OutboxRetryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OutboxTimeoutRecoveryWorkerTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 11, 12, 0);
    private static final long LEASE_TIMEOUT_MS = 60_000L;
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ATTEMPTS = 5;

    @Mock
    NotificationOutboxRepository notificationOutboxRepository;
    @Mock
    NotificationRepository notificationRepository;
    @Mock
    OutboxRecoveryProperties outboxRecoveryProperties;
    @Mock
    OutboxRetryProperties outboxRetryProperties;
    @Mock
    RetryBackoffCalculator retryBackoffCalculator;

    Clock clock = Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    OutboxTimeoutRecoveryWorker worker;

    @BeforeEach
    void setUp() {
        worker = new OutboxTimeoutRecoveryWorker(
                notificationOutboxRepository,
                notificationRepository,
                outboxRecoveryProperties,
                outboxRetryProperties,
                retryBackoffCalculator,
                clock
        );
        lenient().when(outboxRecoveryProperties.leaseTimeoutMs()).thenReturn(LEASE_TIMEOUT_MS);
        lenient().when(outboxRecoveryProperties.batchSize()).thenReturn(BATCH_SIZE);
        lenient().when(outboxRetryProperties.maxAttempts()).thenReturn(MAX_ATTEMPTS);
    }

    @Test
    void stuck_목록이_비어_있으면_아무_상태_변화도_없다() {
        given(notificationOutboxRepository.findRecoverableForUpdate(any(), eq(BATCH_SIZE))).willReturn(List.of());

        worker.recoverStuckRows();

        // no exception, no further interactions verified
    }

    @Test
    void attempt_미달_PROCESSING_행은_RETRY_PENDING_으로_복귀하고_failure_reason_에_lease_timeout_이_기록된다() {
        NotificationOutbox stuck = stuckProcessingAtAttempt(1);
        LocalDateTime claimedAt = stuck.getProcessingStartedAt();
        given(notificationOutboxRepository.findRecoverableForUpdate(any(), eq(BATCH_SIZE)))
                .willReturn(List.of(stuck));
        given(retryBackoffCalculator.nextDelay(anyInt())).willReturn(Duration.ofSeconds(5));

        worker.recoverStuckRows();

        assertAll(
                () -> assertThat(stuck.getStatus()).isEqualTo(NotificationOutboxStatus.RETRY_PENDING),
                () -> assertThat(stuck.getFailureReason()).isEqualTo(FailureReasons.leaseTimeout(claimedAt)),
                () -> assertThat(stuck.getNextAttemptAt()).isEqualTo(FIXED_NOW.plusSeconds(5))
        );
    }

    @Test
    void attempt_초과_PROCESSING_행은_FAILED_로_복귀하고_notification_도_같이_갱신된다() {
        NotificationOutbox stuck = stuckProcessingAtAttempt(MAX_ATTEMPTS);
        LocalDateTime claimedAt = stuck.getProcessingStartedAt();
        String expectedReason = FailureReasons.leaseTimeout(claimedAt);
        given(notificationOutboxRepository.findRecoverableForUpdate(any(), eq(BATCH_SIZE)))
                .willReturn(List.of(stuck));

        worker.recoverStuckRows();

        assertAll(
                () -> assertThat(stuck.getStatus()).isEqualTo(NotificationOutboxStatus.FAILED),
                () -> assertThat(stuck.getFailureReason()).isEqualTo(expectedReason)
        );
        verify(notificationRepository).markFailed(stuck.getNotificationId(), FIXED_NOW, expectedReason);
    }

    private NotificationOutbox stuckProcessingAtAttempt(int targetAttempt) {
        return OutboxFixtures.processingAtAttempt(
                1L,
                FIXED_NOW.minusHours(1),
                FIXED_NOW.minusMinutes(5),
                targetAttempt
        );
    }
}
