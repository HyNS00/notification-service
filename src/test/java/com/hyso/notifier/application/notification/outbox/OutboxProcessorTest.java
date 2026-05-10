package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.application.notification.outbox.dispatcher.NotificationDispatcherRegistry;
import com.hyso.notifier.application.notification.outbox.exception.TransientFailureException;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxStatus;
import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import com.hyso.notifier.global.config.OutboxRetryProperties;
import com.hyso.notifier.global.config.OutboxWorkerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OutboxProcessorTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 10, 12, 0);
    private static final int BATCH_SIZE = 10;
    private static final int MAX_ATTEMPTS = 5;

    @Mock
    NotificationOutboxRepository notificationOutboxRepository;
    @Mock
    NotificationDispatcherRegistry notificationDispatcherRegistry;
    @Mock
    RetryExceptionClassifier retryExceptionClassifier;
    @Mock
    RetryBackoffCalculator retryBackoffCalculator;
    @Mock
    OutboxWorkerProperties outboxWorkerProperties;
    @Mock
    OutboxRetryProperties outboxRetryProperties;

    Clock clock = Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    OutboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new OutboxProcessor(
                notificationOutboxRepository,
                notificationDispatcherRegistry,
                retryExceptionClassifier,
                retryBackoffCalculator,
                outboxWorkerProperties,
                outboxRetryProperties,
                clock
        );
        lenient().when(outboxWorkerProperties.batchSize()).thenReturn(BATCH_SIZE);
        lenient().when(outboxRetryProperties.maxAttempts()).thenReturn(MAX_ATTEMPTS);
        lenient().when(notificationOutboxRepository.saveIfLeaseMatched(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }

    @Test
    void claimBatch_은_findClaimable_결과의_각_outbox_에_claim_을_호출하고_ClaimedOutbox_리스트를_반환한다() {
        NotificationOutbox first = pendingOutbox();
        NotificationOutbox second = pendingOutbox();
        given(notificationOutboxRepository.findClaimableForUpdate(FIXED_NOW, BATCH_SIZE))
                .willReturn(List.of(first, second));

        List<ClaimedOutbox> claimed = processor.claimBatch();

        assertAll(
                () -> assertThat(claimed).hasSize(2),
                () -> assertThat(first.getStatus()).isEqualTo(NotificationOutboxStatus.PROCESSING),
                () -> assertThat(second.getStatus()).isEqualTo(NotificationOutboxStatus.PROCESSING),
                () -> assertThat(claimed.get(0).claimedProcessingStartedAt()).isEqualTo(FIXED_NOW),
                () -> assertThat(claimed.get(1).claimedProcessingStartedAt()).isEqualTo(FIXED_NOW)
        );
    }

    @Test
    void claimBatch_은_빈_결과를_받으면_빈_리스트를_반환한다() {
        given(notificationOutboxRepository.findClaimableForUpdate(FIXED_NOW, BATCH_SIZE))
                .willReturn(List.of());

        List<ClaimedOutbox> claimed = processor.claimBatch();

        assertThat(claimed).isEmpty();
    }

    @Test
    void dispatchAndPersist_는_dispatch_성공_시_outbox_를_SENT_로_전이한다() {
        ClaimedOutbox claimed = claimedFromPending();

        processor.dispatchAndPersist(claimed);

        NotificationOutbox outbox = claimed.outbox();
        assertAll(
                () -> assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.SENT),
                () -> assertThat(outbox.getSentAt()).isEqualTo(FIXED_NOW)
        );
    }

    @Test
    void dispatchAndPersist_는_retryable_실패_attempt_미달_시_RETRY_PENDING_으로_전이한다() {
        ClaimedOutbox claimed = claimedFromPending();
        TransientFailureException cause = new TransientFailureException("일시 장애");
        willThrow(cause).given(notificationDispatcherRegistry).dispatch(eq(NotificationChannel.EMAIL), eq(claimed.outbox()));
        given(retryExceptionClassifier.classify(cause)).willReturn(RetryExceptionClassifier.Classification.RETRYABLE);
        given(retryBackoffCalculator.nextDelay(anyInt())).willReturn(Duration.ofSeconds(5));

        processor.dispatchAndPersist(claimed);

        NotificationOutbox outbox = claimed.outbox();
        assertAll(
                () -> assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.RETRY_PENDING),
                () -> assertThat(outbox.getFailedAt()).isEqualTo(FIXED_NOW),
                () -> assertThat(outbox.getFailureReason()).isEqualTo("TransientFailureException: 일시 장애"),
                () -> assertThat(outbox.getNextAttemptAt()).isEqualTo(FIXED_NOW.plusSeconds(5))
        );
    }

    @Test
    void dispatchAndPersist_는_retryable_실패_attempt_초과_시_FAILED_로_전이하고_사유에_max_attempts_가_포함된다() {
        ClaimedOutbox claimed = claimedFromPendingAtAttempt(MAX_ATTEMPTS);
        TransientFailureException cause = new TransientFailureException("계속 실패");
        willThrow(cause).given(notificationDispatcherRegistry).dispatch(eq(NotificationChannel.EMAIL), eq(claimed.outbox()));
        given(retryExceptionClassifier.classify(cause)).willReturn(RetryExceptionClassifier.Classification.RETRYABLE);

        processor.dispatchAndPersist(claimed);

        NotificationOutbox outbox = claimed.outbox();
        assertAll(
                () -> assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.FAILED),
                () -> assertThat(outbox.getFailureReason()).startsWith("max attempts exceeded (" + MAX_ATTEMPTS + ")")
        );
    }

    @Test
    void dispatchAndPersist_는_non_retryable_실패_시_즉시_FAILED_로_전이한다() {
        ClaimedOutbox claimed = claimedFromPending();
        IllegalArgumentException cause = new IllegalArgumentException("잘못된 입력");
        willThrow(cause).given(notificationDispatcherRegistry).dispatch(eq(NotificationChannel.EMAIL), eq(claimed.outbox()));
        given(retryExceptionClassifier.classify(cause)).willReturn(RetryExceptionClassifier.Classification.NON_RETRYABLE);

        processor.dispatchAndPersist(claimed);

        NotificationOutbox outbox = claimed.outbox();
        assertAll(
                () -> assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.FAILED),
                () -> assertThat(outbox.getFailureReason()).isEqualTo("IllegalArgumentException: 잘못된 입력")
        );
    }

    @Test
    void dispatchAndPersist_는_lease_가_유실되어_saveIfLeaseMatched_가_false_여도_예외를_던지지_않는다() {
        ClaimedOutbox claimed = claimedFromPending();
        given(notificationOutboxRepository.saveIfLeaseMatched(org.mockito.ArgumentMatchers.any(), eq(claimed.claimedProcessingStartedAt())))
                .willReturn(false);

        processor.dispatchAndPersist(claimed);

        assertThat(claimed.outbox().getStatus()).isEqualTo(NotificationOutboxStatus.SENT);
    }

    private NotificationOutbox pendingOutbox() {
        return NotificationOutbox.create(
                anyId(),
                "c43f82d4a0f6c91a5b2e7d8f9340ab1e6c5d2f8a7b9e0134d6a2c8f5e1b9073d",
                42L,
                NotificationChannel.EMAIL,
                "본문",
                FIXED_NOW.minusMinutes(1)
        );
    }

    private ClaimedOutbox claimedFromPending() {
        return claimedFromPendingAtAttempt(1);
    }

    private ClaimedOutbox claimedFromPendingAtAttempt(int targetAttempt) {
        NotificationOutbox outbox = pendingOutbox();
        for (int i = 1; i < targetAttempt; i++) {
            outbox.claim(FIXED_NOW.minusMinutes(targetAttempt - i + 1));
            outbox.markRetryPending(
                    FIXED_NOW.minusMinutes(targetAttempt - i),
                    "이전 실패",
                    FIXED_NOW.minusMinutes(targetAttempt - i - 1)
            );
        }
        outbox.claim(FIXED_NOW);
        return new ClaimedOutbox(outbox, FIXED_NOW);
    }

    private long nextId = 1L;

    private long anyId() {
        return nextId++;
    }
}
