package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import com.hyso.notifier.global.config.OutboxCleanupProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OutboxCleanupSchedulerTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 11, 12, 0);
    private static final int DISPATCHED_RETENTION_DAYS = 7;
    private static final int FAILED_RETENTION_DAYS = 30;
    private static final int BATCH_SIZE = 500;

    @Mock
    NotificationOutboxRepository notificationOutboxRepository;
    @Mock
    OutboxCleanupProperties outboxCleanupProperties;

    Clock clock = Clock.fixed(FIXED_NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    OutboxCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutboxCleanupScheduler(notificationOutboxRepository, outboxCleanupProperties, clock);
        lenient().when(outboxCleanupProperties.dispatchedRetentionDays()).thenReturn(DISPATCHED_RETENTION_DAYS);
        lenient().when(outboxCleanupProperties.failedRetentionDays()).thenReturn(FAILED_RETENTION_DAYS);
        lenient().when(outboxCleanupProperties.batchSize()).thenReturn(BATCH_SIZE);
    }

    @Test
    void cleanup_은_DISPATCHED_와_FAILED_각각의_retention_cutoff_로_delete_를_호출한다() {
        LocalDateTime expectedDispatchedCutoff = FIXED_NOW.minusDays(DISPATCHED_RETENTION_DAYS);
        LocalDateTime expectedFailedCutoff = FIXED_NOW.minusDays(FAILED_RETENTION_DAYS);
        given(notificationOutboxRepository.deleteDispatchedOlderThan(eq(expectedDispatchedCutoff), eq(BATCH_SIZE))).willReturn(0);
        given(notificationOutboxRepository.deleteFailedOlderThan(eq(expectedFailedCutoff), eq(BATCH_SIZE))).willReturn(0);

        scheduler.cleanup();

        verify(notificationOutboxRepository).deleteDispatchedOlderThan(expectedDispatchedCutoff, BATCH_SIZE);
        verify(notificationOutboxRepository).deleteFailedOlderThan(expectedFailedCutoff, BATCH_SIZE);
    }
}
