package com.hyso.notifier.application.notification;

import com.hyso.notifier.application.IntegrationTest;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.NotificationType;
import com.hyso.notifier.presentation.notification.dto.request.CreateNotificationRequest;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@IntegrationTest
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationRegisterConcurrencyTest {

    @Autowired
    NotificationService notificationService;

    @Autowired
    IdempotencyKeyGenerator idempotencyKeyGenerator;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void 같은_요청을_동시에_등록해도_한_행만_저장되고_같은_id를_반환한다() throws Exception {
        CreateNotificationRequest request = request();
        String idempotencyKey = idempotencyKey(request);
        int threadCount = 10;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RegisterNotificationResult>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return notificationService.register(request);
                }));
            }
            ready.await();
            start.countDown();
        }

        List<RegisterNotificationResult> results = new ArrayList<>();
        for (Future<RegisterNotificationResult> future : futures) {
            results.add(future.get(5, TimeUnit.SECONDS));
        }

        Long expectedId = results.get(0).id();
        assertAll(
                () -> assertThat(results)
                        .extracting(RegisterNotificationResult::id)
                        .containsOnly(expectedId),
                () -> assertThat(results)
                        .filteredOn(RegisterNotificationResult::created)
                        .hasSize(1),
                () -> assertThat(countNotifications(idempotencyKey)).isEqualTo(1L),
                () -> assertThat(countOutboxes(idempotencyKey)).isEqualTo(1L)
        );
    }

    private CreateNotificationRequest request() {
        return new CreateNotificationRequest(
                1L,
                NotificationType.ENROLLMENT_COMPLETED,
                NotificationChannel.EMAIL,
                "ENROLLMENT",
                100L
        );
    }

    private String idempotencyKey(CreateNotificationRequest request) {
        return idempotencyKeyGenerator.generate(
                request.receiverId(),
                request.type(),
                request.refType(),
                request.refId(),
                request.channel()
        );
    }

    private Long countNotifications(String idempotencyKey) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE idempotency_key = ?",
                Long.class,
                idempotencyKey
        );
    }

    private Long countOutboxes(String idempotencyKey) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification_outboxes WHERE idempotency_key = ?",
                Long.class,
                idempotencyKey
        );
    }
}
