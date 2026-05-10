package com.hyso.notifier.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("outbox.retry")
public record OutboxRetryProperties(
        int maxAttempts,
        long baseDelayMs,
        long multiplier,
        long maxDelayMs,
        double jitterRatio
) {
}
