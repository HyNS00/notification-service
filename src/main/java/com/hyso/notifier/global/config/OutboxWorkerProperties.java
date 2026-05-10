package com.hyso.notifier.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("outbox.worker")
public record OutboxWorkerProperties(
        boolean autoStart,
        int batchSize,
        long pollMinDelayMs,
        long pollMaxDelayMs,
        long pollMultiplier,
        double pollJitterRatio
) {
}
