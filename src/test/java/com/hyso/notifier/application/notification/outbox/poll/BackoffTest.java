package com.hyso.notifier.application.notification.outbox.poll;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BackoffTest {

    @Test
    void IDLE_상태가_이어지면_폴링_지연이_min에서_max까지_지수적으로_늘어난다() {
        Backoff b = new Backoff(
                Duration.ofMillis(100),
                Duration.ofMillis(500),
                2,
                0.0,
                new Random(0)
        );

        assertAll(
                () -> assertThat(b.next()).isEqualTo(Duration.ofMillis(100)),
                () -> assertThat(b.next()).isEqualTo(Duration.ofMillis(200)),
                () -> assertThat(b.next()).isEqualTo(Duration.ofMillis(400)),
                () -> assertThat(b.next()).isEqualTo(Duration.ofMillis(500)),
                () -> assertThat(b.next()).isEqualTo(Duration.ofMillis(500))
        );
    }

    @Test
    void reset_을_부르면_다음_지연이_다시_min이_된다() {
        Backoff b = new Backoff(
                Duration.ofMillis(100),
                Duration.ofSeconds(5),
                2,
                0.0,
                new Random(0)
        );
        b.next();
        b.next();
        b.next();

        b.reset();

        assertThat(b.next()).isEqualTo(Duration.ofMillis(100));
    }

    @Test
    void 최대_지연이_최소보다_작으면_생성에_실패한다() {
        assertThatThrownBy(() -> new Backoff(
                Duration.ofSeconds(10),
                Duration.ofSeconds(5),
                2,
                0.0,
                new Random(0)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("최대 폴링 간격은 최소 간격 이상이어야 합니다.");
    }
}
