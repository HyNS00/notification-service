package com.hyso.notifier.application.notification.outbox;

import java.time.Duration;
import java.util.Random;

public class RetryBackoffCalculator {

    private final long baseMillis;
    private final long multiplier;
    private final long maxMillis;
    private final double jitterRatio;
    private final Random random;

    public RetryBackoffCalculator(
            Duration baseDelay,
            long multiplier,
            Duration maxDelay,
            double jitterRatio,
            Random random
    ) {
        validate(baseDelay, multiplier, maxDelay, jitterRatio, random);
        this.baseMillis = baseDelay.toMillis();
        this.multiplier = multiplier;
        this.maxMillis = maxDelay.toMillis();
        this.jitterRatio = jitterRatio;
        this.random = random;
    }

    public Duration nextDelay(int attempt) {
        validateAttempt(attempt);
        long unjittered = computeExponential(attempt);
        double jitterFactor = 1.0 + (random.nextDouble() * 2.0 - 1.0) * jitterRatio;
        long jittered = (long) (unjittered * jitterFactor);
        return Duration.ofMillis(Math.max(0L, jittered));
    }

    private long computeExponential(int attempt) {
        long delay = baseMillis;
        for (int i = 1; i < attempt; i++) {
            if (delay > maxMillis / multiplier) {
                return maxMillis;
            }
            delay = delay * multiplier;
        }
        return Math.min(delay, maxMillis);
    }

    private static void validate(
            Duration baseDelay,
            long multiplier,
            Duration maxDelay,
            double jitterRatio,
            Random random
    ) {
        validateBaseDelay(baseDelay);
        validateMultiplier(multiplier);
        validateMaxDelay(maxDelay, baseDelay);
        validateJitterRatio(jitterRatio);
        validateRandom(random);
    }

    private static void validateBaseDelay(Duration baseDelay) {
        if (baseDelay == null || baseDelay.isNegative() || baseDelay.isZero()) {
            throw new IllegalArgumentException("기본 재시도 지연 시간은 양수여야 합니다.");
        }
    }

    private static void validateMultiplier(long multiplier) {
        if (multiplier < 1) {
            throw new IllegalArgumentException("재시도 배수는 1 이상이어야 합니다.");
        }
    }

    private static void validateMaxDelay(Duration maxDelay, Duration baseDelay) {
        if (maxDelay == null) {
            throw new IllegalArgumentException("최대 재시도 지연 시간은 비어 있을 수 없습니다.");
        }
        if (baseDelay != null && maxDelay.compareTo(baseDelay) < 0) {
            throw new IllegalArgumentException("최대 재시도 지연 시간은 기본 지연 시간 이상이어야 합니다.");
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

    private static void validateAttempt(int attempt) {
        if (attempt < 1) {
            throw new IllegalArgumentException("시도 횟수는 1 이상이어야 합니다.");
        }
    }
}
