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
class NotificationOutboxClaimQueryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 10, 12, 0);

    @Autowired
    NotificationOutboxRepository notificationOutboxRepository;

    @Test
    @Sql("/sql/outbox/claim_candidates.sql")
    void claimable_상태이고_next_attempt_at_이_지나거나_NULL인_행만_id_오름차순으로_반환된다() {
        List<NotificationOutbox> claimed = notificationOutboxRepository.findClaimableForUpdate(NOW, 10);

        assertThat(claimed)
                .extracting(NotificationOutbox::getId)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    @Sql("/sql/outbox/claim_candidates.sql")
    void batchSize_보다_claimable_행이_많아도_batchSize_만큼만_반환된다() {
        List<NotificationOutbox> claimed = notificationOutboxRepository.findClaimableForUpdate(NOW, 2);

        assertThat(claimed)
                .extracting(NotificationOutbox::getId)
                .containsExactly(1L, 2L);
    }
}
