package com.hyso.notifier.application.notification;

import com.hyso.notifier.infrastructure.notification.NotificationType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationMessageRendererTest {

    private final NotificationMessageRenderer renderer = new NotificationMessageRenderer();

    @Test
    void 타입별_정확한_메시지를_반환한다() {
        assertAll(
                () -> assertThat(renderer.render(NotificationType.ENROLLMENT_COMPLETED))
                        .isEqualTo("수강 신청이 완료되었습니다."),
                () -> assertThat(renderer.render(NotificationType.PAYMENT_CONFIRMED))
                        .isEqualTo("결제가 승인되었습니다."),
                () -> assertThat(renderer.render(NotificationType.COURSE_START_D1))
                        .isEqualTo("내일 강의가 시작됩니다."),
                () -> assertThat(renderer.render(NotificationType.ENROLLMENT_CANCELLED))
                        .isEqualTo("수강 신청이 취소되었습니다.")
        );
    }

    @Test
    void type이_비어있으면_IllegalArgumentException을_던진다() {
        assertThatThrownBy(() -> renderer.render(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("알림 타입은 비어 있을 수 없습니다.");
    }
}
