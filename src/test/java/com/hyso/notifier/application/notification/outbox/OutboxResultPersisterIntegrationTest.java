package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.application.IntegrationTest;
import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxStatus;
import com.hyso.notifier.infrastructure.notification.persistence.JpaNotificationRepository;
import com.hyso.notifier.infrastructure.notification.persistence.outbox.JpaNotificationOutboxRepository;
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
class OutboxResultPersisterIntegrationTest {

    private static final LocalDateTime CLAIMED_AT = LocalDateTime.of(2026, 5, 10, 12, 0);
    private static final LocalDateTime SENT_AT = CLAIMED_AT.plusSeconds(3);
    private static final LocalDateTime FAILED_AT = CLAIMED_AT.plusSeconds(2);

    @Autowired
    OutboxResultPersister persister;

    @Autowired
    JpaNotificationOutboxRepository jpaOutboxRepository;

    @Autowired
    JpaNotificationRepository jpaNotificationRepository;

    @Test
    @Sql({"/sql/notification/insert_notification.sql", "/sql/outbox/lease_match_candidate.sql"})
    void SENT_시_outbox_와_notification_이_한_트랜잭션에_같이_커밋된다() {
        NotificationOutbox outbox = jpaOutboxRepository.findById(1L).orElseThrow();
        outbox.markSent(SENT_AT);

        boolean result = persister.persist(outbox, CLAIMED_AT);

        NotificationOutbox afterOutbox = jpaOutboxRepository.findById(1L).orElseThrow();
        Notification afterNotification = jpaNotificationRepository.findById(1L).orElseThrow();
        assertAll(
                () -> assertThat(result).isTrue(),
                () -> assertThat(afterOutbox.getStatus()).isEqualTo(NotificationOutboxStatus.SENT),
                () -> assertThat(afterOutbox.getSentAt()).isEqualTo(SENT_AT),
                () -> assertThat(afterNotification.getSentAt()).isEqualTo(SENT_AT),
                () -> assertThat(afterNotification.getFailedAt()).isNull(),
                () -> assertThat(afterNotification.getFailureReason()).isNull()
        );
    }

    @Test
    @Sql({"/sql/notification/insert_notification.sql", "/sql/outbox/lease_match_candidate.sql"})
    void FAILED_시_outbox_와_notification_이_한_트랜잭션에_같이_커밋된다() {
        NotificationOutbox outbox = jpaOutboxRepository.findById(1L).orElseThrow();
        outbox.markFailed(FAILED_AT, "최종 실패 사유");

        boolean result = persister.persist(outbox, CLAIMED_AT);

        NotificationOutbox afterOutbox = jpaOutboxRepository.findById(1L).orElseThrow();
        Notification afterNotification = jpaNotificationRepository.findById(1L).orElseThrow();
        assertAll(
                () -> assertThat(result).isTrue(),
                () -> assertThat(afterOutbox.getStatus()).isEqualTo(NotificationOutboxStatus.FAILED),
                () -> assertThat(afterOutbox.getFailedAt()).isEqualTo(FAILED_AT),
                () -> assertThat(afterOutbox.getFailureReason()).isEqualTo("최종 실패 사유"),
                () -> assertThat(afterNotification.getFailedAt()).isEqualTo(FAILED_AT),
                () -> assertThat(afterNotification.getFailureReason()).isEqualTo("최종 실패 사유"),
                () -> assertThat(afterNotification.getSentAt()).isNull()
        );
    }

    @Test
    @Sql({"/sql/notification/insert_notification.sql", "/sql/outbox/lease_match_candidate.sql"})
    void RETRY_PENDING_시_outbox_만_갱신되고_notification_은_무변경이다() {
        NotificationOutbox outbox = jpaOutboxRepository.findById(1L).orElseThrow();
        LocalDateTime nextAttempt = CLAIMED_AT.plusSeconds(10);
        outbox.markRetryPending(FAILED_AT, "직전 시도 실패", nextAttempt);

        boolean result = persister.persist(outbox, CLAIMED_AT);

        NotificationOutbox afterOutbox = jpaOutboxRepository.findById(1L).orElseThrow();
        Notification afterNotification = jpaNotificationRepository.findById(1L).orElseThrow();
        assertAll(
                () -> assertThat(result).isTrue(),
                () -> assertThat(afterOutbox.getStatus()).isEqualTo(NotificationOutboxStatus.RETRY_PENDING),
                () -> assertThat(afterOutbox.getNextAttemptAt()).isEqualTo(nextAttempt),
                () -> assertThat(afterOutbox.getFailureReason()).isEqualTo("직전 시도 실패"),
                () -> assertThat(afterNotification.getSentAt()).isNull(),
                () -> assertThat(afterNotification.getFailedAt()).isNull(),
                () -> assertThat(afterNotification.getFailureReason()).isNull()
        );
    }

    @Test
    @Sql({"/sql/notification/insert_notification.sql", "/sql/outbox/lease_match_candidate.sql"})
    void lease_가_유실되면_outbox_와_notification_둘_다_무변경이다() {
        NotificationOutbox outbox = jpaOutboxRepository.findById(1L).orElseThrow();
        outbox.markSent(SENT_AT);
        LocalDateTime mismatchedClaim = CLAIMED_AT.plusMinutes(1);

        boolean result = persister.persist(outbox, mismatchedClaim);

        NotificationOutbox afterOutbox = jpaOutboxRepository.findById(1L).orElseThrow();
        Notification afterNotification = jpaNotificationRepository.findById(1L).orElseThrow();
        assertAll(
                () -> assertThat(result).isFalse(),
                () -> assertThat(afterOutbox.getStatus()).isEqualTo(NotificationOutboxStatus.PROCESSING),
                () -> assertThat(afterOutbox.getSentAt()).isNull(),
                () -> assertThat(afterNotification.getSentAt()).isNull(),
                () -> assertThat(afterNotification.getFailedAt()).isNull(),
                () -> assertThat(afterNotification.getFailureReason()).isNull()
        );
    }
}
