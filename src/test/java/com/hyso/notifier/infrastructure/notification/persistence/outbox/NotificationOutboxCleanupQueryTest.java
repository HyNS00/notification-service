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
class NotificationOutboxCleanupQueryTest {

    private static final LocalDateTime CUTOFF = LocalDateTime.of(2026, 5, 1, 0, 0);

    @Autowired
    NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    JpaNotificationOutboxRepository jpaNotificationOutboxRepository;

    @Test
    @Sql("/sql/outbox/cleanup_candidates.sql")
    void deleteSentOlderThan_은_cutoff_보다_오래된_SENT_행만_삭제하고_다른_상태와_최근_SENT_는_보존한다() {
        int deleted = notificationOutboxRepository.deleteSentOlderThan(CUTOFF, 100);

        List<NotificationOutbox> remaining = jpaNotificationOutboxRepository.findAll();
        assertThat(deleted).isEqualTo(1);
        assertThat(remaining)
                .extracting(NotificationOutbox::getId)
                .containsExactlyInAnyOrder(2L, 3L, 4L, 5L, 6L);
    }

    @Test
    @Sql("/sql/outbox/cleanup_candidates.sql")
    void deleteFailedOlderThan_은_cutoff_보다_오래된_FAILED_행만_삭제하고_다른_상태와_최근_FAILED_는_보존한다() {
        int deleted = notificationOutboxRepository.deleteFailedOlderThan(CUTOFF, 100);

        List<NotificationOutbox> remaining = jpaNotificationOutboxRepository.findAll();
        assertThat(deleted).isEqualTo(1);
        assertThat(remaining)
                .extracting(NotificationOutbox::getId)
                .containsExactlyInAnyOrder(1L, 2L, 4L, 5L, 6L);
    }

    @Test
    @Sql("/sql/outbox/cleanup_candidates.sql")
    void deleteSentOlderThan_은_batchSize_보다_많이_삭제하지_않는다() {
        LocalDateTime farFuture = LocalDateTime.of(2030, 1, 1, 0, 0);

        int deleted = notificationOutboxRepository.deleteSentOlderThan(farFuture, 1);

        assertThat(deleted).isEqualTo(1);
        List<NotificationOutbox> remaining = jpaNotificationOutboxRepository.findAll();
        assertThat(remaining).hasSize(5);
    }
}
