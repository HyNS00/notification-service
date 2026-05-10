package com.hyso.notifier.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("outbox.cleanup")
public record OutboxCleanupProperties(
        int sentRetentionDays,
        int failedRetentionDays,
        long fixedDelayMs,
        int batchSize
) {
}
