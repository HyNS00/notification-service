package com.hyso.notifier.application.notification;

import com.hyso.notifier.infrastructure.notification.NotificationChannel;
import com.hyso.notifier.infrastructure.notification.NotificationType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class IdempotencyKeyGeneratorTest {

    private final IdempotencyKeyGenerator generator = new IdempotencyKeyGenerator();

    @Test
    void 같은_입력은_같은_키를_만든다() {
        String a = generator.generate(1L, NotificationType.ENROLLMENT_COMPLETED, "ENROLLMENT", 100L, NotificationChannel.EMAIL);
        String b = generator.generate(1L, NotificationType.ENROLLMENT_COMPLETED, "ENROLLMENT", 100L, NotificationChannel.EMAIL);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void 키는_64자_소문자_hex_문자열이다() {
        String key = generator.generate(1L, NotificationType.ENROLLMENT_COMPLETED, "ENROLLMENT", 100L, NotificationChannel.EMAIL);
        assertThat(key).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void receiverId가_다르면_키가_달라진다() {
        String a = generator.generate(1L, NotificationType.ENROLLMENT_COMPLETED, "ENROLLMENT", 100L, NotificationChannel.EMAIL);
        String b = generator.generate(2L, NotificationType.ENROLLMENT_COMPLETED, "ENROLLMENT", 100L, NotificationChannel.EMAIL);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void type이_다르면_키가_달라진다() {
        String a = generator.generate(1L, NotificationType.ENROLLMENT_COMPLETED, "ENROLLMENT", 100L, NotificationChannel.EMAIL);
        String b = generator.generate(1L, NotificationType.PAYMENT_CONFIRMED, "ENROLLMENT", 100L, NotificationChannel.EMAIL);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void refType이_다르면_키가_달라진다() {
        String a = generator.generate(1L, NotificationType.ENROLLMENT_COMPLETED, "ENROLLMENT", 100L, NotificationChannel.EMAIL);
        String b = generator.generate(1L, NotificationType.ENROLLMENT_COMPLETED, "PAYMENT", 100L, NotificationChannel.EMAIL);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void refId가_다르면_키가_달라진다() {
        String a = generator.generate(1L, NotificationType.ENROLLMENT_COMPLETED, "ENROLLMENT", 100L, NotificationChannel.EMAIL);
        String b = generator.generate(1L, NotificationType.ENROLLMENT_COMPLETED, "ENROLLMENT", 999L, NotificationChannel.EMAIL);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void channel이_다르면_키가_달라진다() {
        String a = generator.generate(1L, NotificationType.ENROLLMENT_COMPLETED, "ENROLLMENT", 100L, NotificationChannel.EMAIL);
        String b = generator.generate(1L, NotificationType.ENROLLMENT_COMPLETED, "ENROLLMENT", 100L, NotificationChannel.IN_APP);
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void 각_필드가_비어있으면_IllegalArgumentException을_던진다() {
        NotificationType type = NotificationType.ENROLLMENT_COMPLETED;
        NotificationChannel channel = NotificationChannel.EMAIL;

        assertAll(
                () -> assertThatThrownBy(() -> generator.generate(null, type, "ENROLLMENT", 100L, channel))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("수신자 ID 는 비어 있을 수 없습니다."),
                () -> assertThatThrownBy(() -> generator.generate(1L, null, "ENROLLMENT", 100L, channel))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("알림 타입은 비어 있을 수 없습니다."),
                () -> assertThatThrownBy(() -> generator.generate(1L, type, null, 100L, channel))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("참조 타입은 비어 있을 수 없습니다."),
                () -> assertThatThrownBy(() -> generator.generate(1L, type, " ", 100L, channel))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("참조 타입은 비어 있을 수 없습니다."),
                () -> assertThatThrownBy(() -> generator.generate(1L, type, "ENROLLMENT", null, channel))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("참조 ID 는 비어 있을 수 없습니다."),
                () -> assertThatThrownBy(() -> generator.generate(1L, type, "ENROLLMENT", 100L, null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("발송 채널은 비어 있을 수 없습니다.")
        );
    }
}
