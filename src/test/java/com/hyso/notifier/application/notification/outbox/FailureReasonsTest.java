package com.hyso.notifier.application.notification.outbox;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FailureReasonsTest {

    @Test
    void fromException_은_예외_타입_이름과_메시지를_콜론으로_조립한다() {
        IOException cause = new IOException("connection refused");

        String reason = FailureReasons.fromException(cause);

        assertThat(reason).isEqualTo("IOException: connection refused");
    }

    @Test
    void fromException_은_예외_메시지가_없으면_타입_이름만_반환한다() {
        IOException cause = new IOException();

        String reason = FailureReasons.fromException(cause);

        assertThat(reason).isEqualTo("IOException");
    }

    @Test
    void fromException_은_조립_결과가_500자를_넘으면_500자로_자른다() {
        IOException cause = new IOException("x".repeat(1000));

        String reason = FailureReasons.fromException(cause);

        assertThat(reason).hasSize(500);
        assertThat(reason).startsWith("IOException: ");
    }

    @Test
    void maxAttemptsExceeded_는_시도_횟수와_원인을_조립한다() {
        IOException cause = new IOException("timeout");

        String reason = FailureReasons.maxAttemptsExceeded(5, cause);

        assertThat(reason).isEqualTo("max attempts exceeded (5): IOException: timeout");
    }

    @Test
    void leaseTimeout_은_claim_시각을_포함한다() {
        LocalDateTime claimedAt = LocalDateTime.of(2026, 5, 10, 12, 0);

        String reason = FailureReasons.leaseTimeout(claimedAt);

        assertThat(reason).isEqualTo("lease timeout (claimed at 2026-05-10T12:00)");
    }

    @Test
    void 입력이_부적절하면_IllegalArgumentException_을_던진다() {
        assertAll(
                () -> assertThatThrownBy(() -> FailureReasons.fromException(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("예외 원인은 비어 있을 수 없습니다."),
                () -> assertThatThrownBy(() -> FailureReasons.maxAttemptsExceeded(0, new IOException()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("시도 횟수는 1 이상이어야 합니다."),
                () -> assertThatThrownBy(() -> FailureReasons.maxAttemptsExceeded(5, null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("예외 원인은 비어 있을 수 없습니다."),
                () -> assertThatThrownBy(() -> FailureReasons.leaseTimeout(null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("claim 시각은 비어 있을 수 없습니다.")
        );
    }
}
