package com.hyso.notifier.infrastructure.notification.persistence.outbox;

import com.hyso.notifier.application.IntegrationTest;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationOutboxRecoveryQueryTest {

    private static final LocalDateTime CUTOFF = LocalDateTime.of(2026, 5, 11, 11, 59, 0);

    @Autowired
    NotificationOutboxRepository notificationOutboxRepository;

    @Test
    @Sql("/sql/outbox/recovery_candidates.sql")
    void cutoff_보다_오래된_PROCESSING_행만_processing_started_at_오름차순으로_반환된다() {
        List<NotificationOutbox> recoverable = notificationOutboxRepository.findRecoverableForUpdate(CUTOFF, 10);

        assertThat(recoverable)
                .extracting(NotificationOutbox::getId)
                .containsExactly(1L, 2L);
    }

    @Test
    @Sql("/sql/outbox/recovery_candidates.sql")
    void batchSize_보다_recoverable_행이_많아도_batchSize_만큼만_반환된다() {
        List<NotificationOutbox> recoverable = notificationOutboxRepository.findRecoverableForUpdate(CUTOFF, 1);

        assertThat(recoverable)
                .extracting(NotificationOutbox::getId)
                .containsExactly(1L);
    }
}
