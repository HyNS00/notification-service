package com.hyso.notifier.global.config;

import com.hyso.notifier.application.notification.outbox.RetryBackoffCalculator;
import com.hyso.notifier.application.notification.outbox.RetryExceptionClassifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Random;

@Configuration
public class OutboxConfig {

    @Bean
    public RetryExceptionClassifier retryExceptionClassifier() {
        return new RetryExceptionClassifier();
    }

    @Bean
    public RetryBackoffCalculator retryBackoffCalculator(OutboxRetryProperties outboxRetryProperties) {
        return new RetryBackoffCalculator(
                Duration.ofMillis(outboxRetryProperties.baseDelayMs()),
                outboxRetryProperties.multiplier(),
                Duration.ofMillis(outboxRetryProperties.maxDelayMs()),
                outboxRetryProperties.jitterRatio(),
                new Random()
        );
    }
}
