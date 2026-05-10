package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxStatus;
import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import com.hyso.notifier.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OutboxResultPersisterTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 11, 12, 0);

    @Mock
    NotificationOutboxRepository notificationOutboxRepository;
    @Mock
    NotificationRepository notificationRepository;
    @InjectMocks
    OutboxResultPersister persister;

    @Test
    void outbox_저장_성공_후_SENT_이면_notification_도_markSent_된다() {
        NotificationOutbox outbox = OutboxFixtures.processingAtAttempt(99L, FIXED_NOW.minusMinutes(10), FIXED_NOW, 1);
        outbox.markSent(FIXED_NOW);
        given(notificationOutboxRepository.saveIfLeaseMatched(eq(outbox), eq(FIXED_NOW))).willReturn(true);

        boolean result = persister.persist(outbox, FIXED_NOW);

        assertThat(result).isTrue();
        verify(notificationRepository).markSent(99L, FIXED_NOW);
    }

    @Test
    void outbox_저장_성공_후_FAILED_이면_notification_도_markFailed_된다() {
        NotificationOutbox outbox = OutboxFixtures.processingAtAttempt(99L, FIXED_NOW.minusMinutes(10), FIXED_NOW, 1);
        outbox.markFailed(FIXED_NOW, "사유");
        given(notificationOutboxRepository.saveIfLeaseMatched(eq(outbox), eq(FIXED_NOW))).willReturn(true);

        boolean result = persister.persist(outbox, FIXED_NOW);

        assertThat(result).isTrue();
        verify(notificationRepository).markFailed(99L, FIXED_NOW, "사유");
    }

    @Test
    void outbox_저장_성공_후_RETRY_PENDING_이면_notification_은_갱신되지_않는다() {
        NotificationOutbox outbox = OutboxFixtures.processingAtAttempt(99L, FIXED_NOW.minusMinutes(10), FIXED_NOW, 1);
        outbox.markRetryPending(FIXED_NOW, "사유", FIXED_NOW.plusSeconds(5));
        given(notificationOutboxRepository.saveIfLeaseMatched(eq(outbox), eq(FIXED_NOW))).willReturn(true);

        boolean result = persister.persist(outbox, FIXED_NOW);

        assertThat(result).isTrue();
        verify(notificationRepository, never()).markSent(any(), any());
        verify(notificationRepository, never()).markFailed(any(), any(), any());
    }

    @Test
    void lease_가_유실되어_outbox_저장이_실패하면_notification_은_갱신되지_않고_false_를_반환한다() {
        NotificationOutbox outbox = OutboxFixtures.processingAtAttempt(99L, FIXED_NOW.minusMinutes(10), FIXED_NOW, 1);
        outbox.markSent(FIXED_NOW);
        given(notificationOutboxRepository.saveIfLeaseMatched(eq(outbox), eq(FIXED_NOW))).willReturn(false);

        boolean result = persister.persist(outbox, FIXED_NOW);

        assertThat(result).isFalse();
        verify(notificationRepository, never()).markSent(any(), any());
        verify(notificationRepository, never()).markFailed(any(), any(), any());
    }
}
