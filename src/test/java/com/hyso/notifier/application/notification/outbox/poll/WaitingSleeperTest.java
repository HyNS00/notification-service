package com.hyso.notifier.application.notification.outbox.poll;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WaitingSleeperTest {

    @Test
    void 다른_스레드에서_wakeUp을_부르면_sleep이_즉시_종료된다() throws Exception {
        WaitingSleeper sleeper = new WaitingSleeper();
        AtomicLong elapsedMs = new AtomicLong(-1);

        Thread sleepingThread = new Thread(() -> {
            long start = System.nanoTime();
            try {
                sleeper.sleep(Duration.ofSeconds(10));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            elapsedMs.set((System.nanoTime() - start) / 1_000_000);
        });
        sleepingThread.start();
        Thread.sleep(50);

        sleeper.wakeUp();

        sleepingThread.join(2_000);
        assertThat(sleepingThread.isAlive()).isFalse();
        assertThat(elapsedMs.get()).isLessThan(1_000L);
    }

    @Test
    void duration이_0_또는_음수면_sleep은_즉시_반환된다() throws Exception {
        WaitingSleeper sleeper = new WaitingSleeper();

        long start = System.nanoTime();
        sleeper.sleep(Duration.ZERO);
        sleeper.sleep(Duration.ofMillis(-5));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(50L);
    }
}
