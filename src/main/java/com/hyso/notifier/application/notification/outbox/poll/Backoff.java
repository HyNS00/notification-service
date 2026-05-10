package com.hyso.notifier.application.notification.outbox.poll;

import java.time.Duration;
import java.util.Random;

public class Backoff {

    private final long minMillis;
    private final long maxMillis;
    private final long multiplier;
    private final double jitterRatio;
    private final Random random;
    private long currentMillis;

    public Backoff(
            Duration minDelay,
            Duration maxDelay,
            long multiplier,
            double jitterRatio,
            Random random
    ) {
        validate(minDelay, maxDelay, multiplier, jitterRatio, random);
        this.minMillis = minDelay.toMillis();
        this.maxMillis = maxDelay.toMillis();
        this.multiplier = multiplier;
        this.jitterRatio = jitterRatio;
        this.random = random;
        this.currentMillis = this.minMillis;
    }

    public Duration next() {
        long base = currentMillis;
        advance();
        return applyJitter(base);
    }

    public void reset() {
        currentMillis = minMillis;
    }

    private void advance() {
        if (currentMillis >= maxMillis / multiplier) {
            currentMillis = maxMillis;
            return;
        }
        currentMillis = currentMillis * multiplier;
    }

    private Duration applyJitter(long base) {
        double factor = 1.0 + (random.nextDouble() * 2.0 - 1.0) * jitterRatio;
        long jittered = (long) (base * factor);
        return Duration.ofMillis(Math.max(0L, jittered));
    }

    private static void validate(
            Duration minDelay,
            Duration maxDelay,
            long multiplier,
            double jitterRatio,
            Random random
    ) {
        validateMinDelay(minDelay);
        validateMaxDelay(maxDelay, minDelay);
        validateMultiplier(multiplier);
        validateJitterRatio(jitterRatio);
        validateRandom(random);
    }

    private static void validateMinDelay(Duration minDelay) {
        if (minDelay == null || minDelay.isNegative() || minDelay.isZero()) {
            throw new IllegalArgumentException("최소 폴링 간격은 양수여야 합니다.");
        }
    }

    private static void validateMaxDelay(Duration maxDelay, Duration minDelay) {
        if (maxDelay == null) {
            throw new IllegalArgumentException("최대 폴링 간격은 비어 있을 수 없습니다.");
        }
        if (minDelay != null && maxDelay.compareTo(minDelay) < 0) {
            throw new IllegalArgumentException("최대 폴링 간격은 최소 간격 이상이어야 합니다.");
        }
    }

    private static void validateMultiplier(long multiplier) {
        if (multiplier < 1) {
            throw new IllegalArgumentException("폴링 배수는 1 이상이어야 합니다.");
        }
    }

    private static void validateJitterRatio(double jitterRatio) {
        if (jitterRatio < 0.0 || jitterRatio > 1.0) {
            throw new IllegalArgumentException("Jitter 비율은 0 이상 1 이하여야 합니다.");
        }
    }

    private static void validateRandom(Random random) {
        if (random == null) {
            throw new IllegalArgumentException("Random 은 비어 있을 수 없습니다.");
        }
    }
}
