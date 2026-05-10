package com.hyso.notifier.infrastructure.notification.persistence.outbox;

import com.hyso.notifier.application.IntegrationTest;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxLeaseState;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxStatus;
import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@IntegrationTest
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationOutboxLeaseUpdateTest {

    private static final LocalDateTime CLAIMED_AT = LocalDateTime.of(2026, 5, 10, 12, 0);
    private static final LocalDateTime SENT_AT = CLAIMED_AT.plusSeconds(3);

    @Autowired
    NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    JpaNotificationOutboxRepository jpaNotificationOutboxRepository;

    @Test
    @Sql("/sql/outbox/lease_match_candidate.sql")
    void claim_시각이_DB의_lease_와_일치하면_결과_저장이_적용되고_true_를_반환한다() {
        NotificationOutbox outbox = jpaNotificationOutboxRepository.findById(1L).orElseThrow();
        outbox.markSent(SENT_AT);

        boolean updated = notificationOutboxRepository.saveIfLeaseMatched(outbox, CLAIMED_AT);

        NotificationOutbox after = jpaNotificationOutboxRepository.findById(1L).orElseThrow();
        assertAll(
                () -> assertThat(updated).isTrue(),
                () -> assertThat(after.getStatus()).isEqualTo(NotificationOutboxStatus.SENT),
                () -> assertThat(after.getSentAt()).isEqualTo(SENT_AT),
                () -> assertThat(after.getProcessingLeaseState()).isEqualTo(NotificationOutboxLeaseState.IDLE),
                () -> assertThat(after.getProcessingStartedAt()).isNull()
        );
    }

    @Test
    @Sql("/sql/outbox/lease_match_candidate.sql")
    void claim_시각이_DB의_lease_와_다르면_결과_저장이_거부되고_false_를_반환한다() {
        NotificationOutbox outbox = jpaNotificationOutboxRepository.findById(1L).orElseThrow();
        outbox.markSent(SENT_AT);
        LocalDateTime mismatchedClaim = CLAIMED_AT.plusMinutes(1);

        boolean updated = notificationOutboxRepository.saveIfLeaseMatched(outbox, mismatchedClaim);

        NotificationOutbox after = jpaNotificationOutboxRepository.findById(1L).orElseThrow();
        assertAll(
                () -> assertThat(updated).isFalse(),
                () -> assertThat(after.getStatus()).isEqualTo(NotificationOutboxStatus.PROCESSING),
                () -> assertThat(after.getSentAt()).isNull(),
                () -> assertThat(after.getProcessingStartedAt()).isEqualTo(CLAIMED_AT)
        );
    }

    @Test
    @Sql("/sql/outbox/lease_match_inconsistent_status.sql")
    void DB_상태가_PROCESSING_이_아니면_lease_시각이_같아도_결과_저장이_거부된다() {
        NotificationOutbox outbox = jpaNotificationOutboxRepository.findById(1L).orElseThrow();

        boolean updated = notificationOutboxRepository.saveIfLeaseMatched(outbox, CLAIMED_AT);

        NotificationOutbox after = jpaNotificationOutboxRepository.findById(1L).orElseThrow();
        assertAll(
                () -> assertThat(updated).isFalse(),
                () -> assertThat(after.getStatus()).isEqualTo(NotificationOutboxStatus.RETRY_PENDING)
        );
    }
}
