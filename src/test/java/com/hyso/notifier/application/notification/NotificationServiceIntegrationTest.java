package com.hyso.notifier.application.notification;

import com.hyso.notifier.application.IntegrationTest;
import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.NotificationType;
import com.hyso.notifier.domain.notification.repository.NotificationRepository;
import com.hyso.notifier.infrastructure.notification.exception.NotificationNotFoundException;
import com.hyso.notifier.presentation.notification.dto.request.CreateNotificationRequest;
import com.hyso.notifier.presentation.notification.dto.response.NotificationListResponse;
import com.hyso.notifier.presentation.notification.dto.response.NotificationResponse;
import com.hyso.notifier.presentation.notification.dto.response.NotificationStatus;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@IntegrationTest
@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationServiceIntegrationTest {

    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    IdempotencyKeyGenerator idempotencyKeyGenerator;

    @Test
    void 새_요청을_등록하면_요청_값과_렌더링된_본문으로_알림이_저장된다() {
        CreateNotificationRequest request = request();
        String idempotencyKey = idempotencyKey(request);

        RegisterNotificationResult result = notificationService.register(request);

        Notification saved = notificationRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        assertAll(
                () -> assertThat(result.id()).isEqualTo(saved.getId()),
                () -> assertThat(result.created()).isTrue(),
                () -> assertThat(saved.getReceiverId()).isEqualTo(request.receiverId()),
                () -> assertThat(saved.getType()).isEqualTo(request.type()),
                () -> assertThat(saved.getChannel()).isEqualTo(request.channel()),
                () -> assertThat(saved.getRefType()).isEqualTo(request.refType()),
                () -> assertThat(saved.getRefId()).isEqualTo(request.refId()),
                () -> assertThat(saved.getBody()).isEqualTo("수강 신청이 완료되었습니다."),
                () -> assertThat(saved.getIdempotencyKey()).isEqualTo(idempotencyKey)
        );
    }

    @Test
    void 같은_요청을_다시_등록하면_기존_id를_그대로_반환한다() {
        CreateNotificationRequest request = request();

        RegisterNotificationResult first = notificationService.register(request);
        RegisterNotificationResult second = notificationService.register(request);

        assertAll(
                () -> assertThat(second.id()).isEqualTo(first.id()),
                () -> assertThat(second.created()).isFalse()
        );
    }

    @Sql("/sql/notification/insert_notifications_for_paging.sql")
    @Test
    void 본인_소유_알림을_조회하면_DB_값과_일치하는_응답을_반환한다() {
        NotificationResponse actual = notificationService.findOne(1L, 1L);

        assertAll(
                () -> assertThat(actual.id()).isEqualTo(1L),
                () -> assertThat(actual.type()).isEqualTo(NotificationType.ENROLLMENT_COMPLETED),
                () -> assertThat(actual.channel()).isEqualTo(NotificationChannel.IN_APP),
                () -> assertThat(actual.refType()).isEqualTo("ENROLLMENT"),
                () -> assertThat(actual.refId()).isEqualTo(100L),
                () -> assertThat(actual.body()).isEqualTo("수강 신청이 완료되었습니다."),
                () -> assertThat(actual.status()).isEqualTo(NotificationStatus.PENDING),
                () -> assertThat(actual.sentAt()).isNull(),
                () -> assertThat(actual.failedAt()).isNull(),
                () -> assertThat(actual.readAt()).isNull()
        );
    }

    @Sql("/sql/notification/insert_notifications_for_paging.sql")
    @Test
    void 타인_소유_알림을_조회하면_NotificationNotFoundException을_던진다() {
        assertThatThrownBy(() -> notificationService.findOne(1L, 6L))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Sql("/sql/notification/insert_notifications_for_paging.sql")
    @Test
    void 목록을_조회하면_본인_알림만_최신순으로_반환한다() {
        NotificationListResponse actual = notificationService.findList(1L, null, 10);

        assertThat(actual.items())
                .extracting(NotificationResponse::id)
                .containsExactly(5L, 4L, 3L, 2L, 1L);
    }

    @Sql("/sql/notification/insert_notifications_for_paging.sql")
    @Test
    void read_true_필터는_읽은_알림만_반환한다() {
        NotificationListResponse actual = notificationService.findList(1L, true, 10);

        assertThat(actual.items())
                .extracting(NotificationResponse::id)
                .containsExactly(2L);
    }

    @Sql("/sql/notification/insert_notifications_for_paging.sql")
    @Test
    void read_false_필터는_읽지_않은_알림만_반환한다() {
        NotificationListResponse actual = notificationService.findList(1L, false, 10);

        assertThat(actual.items())
                .extracting(NotificationResponse::id)
                .containsExactly(5L, 4L, 3L, 1L);
    }

    @Sql("/sql/notification/insert_notifications_for_paging.sql")
    @Test
    void 타인_알림은_목록에_노출되지_않는다() {
        NotificationListResponse actual = notificationService.findList(1L, null, 100);

        assertThat(actual.items())
                .extracting(NotificationResponse::id)
                .doesNotContain(6L, 7L);
    }

    @Sql("/sql/notification/insert_notifications_for_paging.sql")
    @Test
    void limit_만큼_최신순으로_반환한다() {
        NotificationListResponse actual = notificationService.findList(1L, null, 3);

        assertThat(actual.items())
                .extracting(NotificationResponse::id)
                .containsExactly(5L, 4L, 3L);
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
}
