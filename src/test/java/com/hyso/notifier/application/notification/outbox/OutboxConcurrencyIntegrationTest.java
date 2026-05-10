package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.application.IntegrationTest;
import com.hyso.notifier.application.notification.NotificationService;
import com.hyso.notifier.application.notification.RegisterNotificationResult;
import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.NotificationType;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.NotificationOutboxStatus;
import com.hyso.notifier.infrastructure.notification.persistence.JpaNotificationRepository;
import com.hyso.notifier.infrastructure.notification.persistence.outbox.JpaNotificationOutboxRepository;
import com.hyso.notifier.presentation.notification.dto.request.CreateNotificationRequest;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@IntegrationTest
@TestPropertySource(properties = "outbox.worker.batch-size=5")
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OutboxConcurrencyIntegrationTest {

    @Autowired
    OutboxProcessor outboxProcessor;

    @Autowired
    NotificationService notificationService;

    @Autowired
    JpaNotificationOutboxRepository jpaNotificationOutboxRepository;

    @Autowired
    JpaNotificationRepository jpaNotificationRepository;

    @Test
    @Sql("/sql/outbox/concurrent_claim_seed.sql")
    void 두_워커가_동시에_claim_해도_같은_row_를_겹쳐서_가져가지_않는다() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch barrier = new CountDownLatch(1);

        Callable<List<Long>> claimTask = () -> {
            barrier.await();
            return outboxProcessor.claimBatch().stream()
                    .map(c -> c.outbox().getId())
                    .toList();
        };

        Future<List<Long>> futureA = executor.submit(claimTask);
        Future<List<Long>> futureB = executor.submit(claimTask);
        barrier.countDown();

        List<Long> idsA = futureA.get(5, TimeUnit.SECONDS);
        List<Long> idsB = futureB.get(5, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertAll(
                () -> assertThat(Collections.disjoint(idsA, idsB))
                        .as("두 워커의 claim 결과는 겹치면 안 된다")
                        .isTrue(),
                () -> assertThat(idsA.size() + idsB.size())
                        .as("총 claim 한 row 수는 시드 row 수를 넘지 않는다")
                        .isLessThanOrEqualTo(10),
                () -> assertThat(idsA.size() + idsB.size())
                        .as("적어도 일부 row 는 claim 되어야 한다")
                        .isGreaterThan(0)
        );
    }

    @Test
    void 등록부터_dispatch_까지_outbox_와_notification_의_결과가_일관_갱신된다() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                42L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                999L
        );
        RegisterNotificationResult registered = notificationService.register(request);

        List<ClaimedOutbox> claimed = outboxProcessor.claimBatch();
        assertThat(claimed).hasSize(1);
        outboxProcessor.dispatchAndPersist(claimed.get(0));

        Notification notification = jpaNotificationRepository.findById(registered.id()).orElseThrow();
        NotificationOutbox outbox = jpaNotificationOutboxRepository.findById(claimed.get(0).outbox().getId()).orElseThrow();

        assertAll(
                () -> assertThat(outbox.getStatus()).isEqualTo(NotificationOutboxStatus.SENT),
                () -> assertThat(outbox.getSentAt()).isNotNull(),
                () -> assertThat(notification.getSentAt()).isNotNull(),
                () -> assertThat(notification.getFailedAt()).isNull(),
                () -> assertThat(notification.getFailureReason()).isNull()
        );
    }
}
