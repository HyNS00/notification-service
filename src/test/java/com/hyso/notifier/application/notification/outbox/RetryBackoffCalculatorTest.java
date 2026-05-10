package com.hyso.notifier.application.notification.outbox;

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
class RetryBackoffCalculatorTest {

    private static RetryBackoffCalculator calculator(
            Duration base,
            long multiplier,
            Duration max,
            double jitterRatio,
            double fixedRandom
    ) {
        return new RetryBackoffCalculator(base, multiplier, max, jitterRatio, new FixedRandom(fixedRandom));
    }

    @Test
    void 첫_시도는_base_지연이다() {
        RetryBackoffCalculator c = calculator(Duration.ofSeconds(5), 2, Duration.ofMinutes(5), 0.0, 0.5);
        assertThat(c.nextDelay(1)).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void 시도가_누적되면_지수적으로_증가한다() {
        RetryBackoffCalculator c = calculator(Duration.ofSeconds(1), 2, Duration.ofHours(1), 0.0, 0.5);
        assertAll(
                () -> assertThat(c.nextDelay(1)).isEqualTo(Duration.ofSeconds(1)),
                () -> assertThat(c.nextDelay(2)).isEqualTo(Duration.ofSeconds(2)),
                () -> assertThat(c.nextDelay(3)).isEqualTo(Duration.ofSeconds(4)),
                () -> assertThat(c.nextDelay(4)).isEqualTo(Duration.ofSeconds(8)),
                () -> assertThat(c.nextDelay(5)).isEqualTo(Duration.ofSeconds(16))
        );
    }

    @Test
    void 지수_증가가_max_지연에서_clamp된다() {
        RetryBackoffCalculator c = calculator(Duration.ofSeconds(1), 2, Duration.ofSeconds(5), 0.0, 0.5);
        assertAll(
                () -> assertThat(c.nextDelay(1)).isEqualTo(Duration.ofSeconds(1)),
                () -> assertThat(c.nextDelay(2)).isEqualTo(Duration.ofSeconds(2)),
                () -> assertThat(c.nextDelay(3)).isEqualTo(Duration.ofSeconds(4)),
                () -> assertThat(c.nextDelay(4)).isEqualTo(Duration.ofSeconds(5)),
                () -> assertThat(c.nextDelay(10)).isEqualTo(Duration.ofSeconds(5))
        );
    }

    @Test
    void jitter_0_이면_random값과_무관하게_정확한_값이다() {
        RetryBackoffCalculator a = calculator(Duration.ofSeconds(1), 2, Duration.ofMinutes(5), 0.0, 0.0);
        RetryBackoffCalculator b = calculator(Duration.ofSeconds(1), 2, Duration.ofMinutes(5), 0.0, 0.999);
        assertThat(a.nextDelay(3)).isEqualTo(b.nextDelay(3)).isEqualTo(Duration.ofSeconds(4));
    }

    @Test
    void jitter_random이_0이면_base에_1빼기_jitter_비율이_곱해진다() {
        RetryBackoffCalculator c = calculator(Duration.ofMillis(1000), 2, Duration.ofMinutes(5), 0.2, 0.0);
        assertThat(c.nextDelay(1)).isEqualTo(Duration.ofMillis(800));
    }

    @Test
    void jitter_random이_0_5이면_변동이_없다() {
        RetryBackoffCalculator c = calculator(Duration.ofMillis(1000), 2, Duration.ofMinutes(5), 0.2, 0.5);
        assertThat(c.nextDelay(1)).isEqualTo(Duration.ofMillis(1000));
    }

    @Test
    void 같은_시드면_결정적이다() {
        RetryBackoffCalculator a = new RetryBackoffCalculator(
                Duration.ofSeconds(1), 2, Duration.ofMinutes(5), 0.2, new Random(42L)
        );
        RetryBackoffCalculator b = new RetryBackoffCalculator(
                Duration.ofSeconds(1), 2, Duration.ofMinutes(5), 0.2, new Random(42L)
        );
        assertAll(
                () -> assertThat(a.nextDelay(1)).isEqualTo(b.nextDelay(1)),
                () -> assertThat(a.nextDelay(2)).isEqualTo(b.nextDelay(2)),
                () -> assertThat(a.nextDelay(3)).isEqualTo(b.nextDelay(3))
        );
    }

    @Test
    void attempt가_0_이하이면_IllegalArgumentException을_던진다() {
        RetryBackoffCalculator c = calculator(Duration.ofSeconds(1), 2, Duration.ofMinutes(5), 0.0, 0.5);
        assertAll(
                () -> assertThatThrownBy(() -> c.nextDelay(0))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("시도 횟수는 1 이상이어야 합니다."),
                () -> assertThatThrownBy(() -> c.nextDelay(-1))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("시도 횟수는 1 이상이어야 합니다.")
        );
    }

    @Test
    void 생성자_인자가_부적절하면_IllegalArgumentException을_던진다() {
        Duration base = Duration.ofSeconds(1);
        Duration max = Duration.ofMinutes(5);
        Random random = new Random(0);

        assertAll(
                () -> assertThatThrownBy(() -> new RetryBackoffCalculator(null, 2, max, 0.2, random))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("기본 재시도 지연 시간은 양수여야 합니다."),
                () -> assertThatThrownBy(() -> new RetryBackoffCalculator(Duration.ZERO, 2, max, 0.2, random))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("기본 재시도 지연 시간은 양수여야 합니다."),
                () -> assertThatThrownBy(() -> new RetryBackoffCalculator(Duration.ofSeconds(-1), 2, max, 0.2, random))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("기본 재시도 지연 시간은 양수여야 합니다."),
                () -> assertThatThrownBy(() -> new RetryBackoffCalculator(base, 0, max, 0.2, random))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("재시도 배수는 1 이상이어야 합니다."),
                () -> assertThatThrownBy(() -> new RetryBackoffCalculator(base, 2, null, 0.2, random))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("최대 재시도 지연 시간은 비어 있을 수 없습니다."),
                () -> assertThatThrownBy(() -> new RetryBackoffCalculator(Duration.ofSeconds(10), 2, Duration.ofSeconds(5), 0.2, random))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("최대 재시도 지연 시간은 기본 지연 시간 이상이어야 합니다."),
                () -> assertThatThrownBy(() -> new RetryBackoffCalculator(base, 2, max, -0.1, random))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Jitter 비율은 0 이상 1 이하여야 합니다."),
                () -> assertThatThrownBy(() -> new RetryBackoffCalculator(base, 2, max, 1.1, random))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Jitter 비율은 0 이상 1 이하여야 합니다."),
                () -> assertThatThrownBy(() -> new RetryBackoffCalculator(base, 2, max, 0.2, null))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Random 은 비어 있을 수 없습니다.")
        );
    }

    private static class FixedRandom extends Random {
        private final double fixed;

        FixedRandom(double fixed) {
            this.fixed = fixed;
        }

        @Override
        public double nextDouble() {
            return fixed;
        }
    }
}
