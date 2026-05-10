package com.hyso.notifier.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("outbox.recovery")
public record OutboxRecoveryProperties(
        long leaseTimeoutMs,
        long fixedDelayMs,
        int batchSize
) {
}
