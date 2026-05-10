package com.hyso.notifier.global.config;

import com.hyso.notifier.application.notification.outbox.RetryBackoffCalculator;
import com.hyso.notifier.application.notification.outbox.RetryExceptionClassifier;
import com.hyso.notifier.application.notification.outbox.poll.AdaptivePollingRunner;
import com.hyso.notifier.application.notification.outbox.poll.Backoff;
import com.hyso.notifier.application.notification.outbox.poll.Sleeper;
import com.hyso.notifier.application.notification.outbox.poll.WaitingSleeper;
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

    @Bean
    public Sleeper outboxSleeper() {
        return new WaitingSleeper();
    }

    @Bean
    public Backoff outboxBackoff(OutboxWorkerProperties outboxWorkerProperties) {
        return new Backoff(
                Duration.ofMillis(outboxWorkerProperties.pollMinDelayMs()),
                Duration.ofMillis(outboxWorkerProperties.pollMaxDelayMs()),
                outboxWorkerProperties.pollMultiplier(),
                outboxWorkerProperties.pollJitterRatio(),
                new Random()
        );
    }

    @Bean
    public AdaptivePollingRunner adaptivePollingRunner(Backoff outboxBackoff, Sleeper outboxSleeper) {
        return new AdaptivePollingRunner(outboxBackoff, outboxSleeper);
    }
}
