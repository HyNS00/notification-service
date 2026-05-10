package com.hyso.notifier.application.notification.outbox.poll;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdaptivePollingRunnerTest {

    private Backoff newBackoff() {
        return new Backoff(
                Duration.ofMillis(100),
                Duration.ofMillis(800),
                2,
                0.0,
                new Random(0)
        );
    }

    @Test
    void IDLE_사이클이_누적되면_runner가_지연을_점점_길게_요청한다() throws Exception {
        FakeSleeper sleeper = new FakeSleeper();
        AdaptivePollingRunner runner = new AdaptivePollingRunner(newBackoff(), sleeper);

        runner.runOnce(() -> PollingHint.IDLE);
        runner.runOnce(() -> PollingHint.IDLE);
        runner.runOnce(() -> PollingHint.IDLE);
        runner.runOnce(() -> PollingHint.IDLE);

        assertThat(sleeper.requestedSleeps).containsExactly(
                Duration.ofMillis(100),
                Duration.ofMillis(200),
                Duration.ofMillis(400),
                Duration.ofMillis(800)
        );
    }

    @Test
    void HAS_WORK_사이클이_오면_runner가_다음_지연을_min으로_되돌린다() throws Exception {
        FakeSleeper sleeper = new FakeSleeper();
        AdaptivePollingRunner runner = new AdaptivePollingRunner(newBackoff(), sleeper);

        runner.runOnce(() -> PollingHint.IDLE);
        runner.runOnce(() -> PollingHint.IDLE);
        runner.runOnce(() -> PollingHint.HAS_WORK);
        runner.runOnce(() -> PollingHint.IDLE);

        assertThat(sleeper.requestedSleeps).containsExactly(
                Duration.ofMillis(100),
                Duration.ofMillis(200),
                Duration.ofMillis(100),
                Duration.ofMillis(200)
        );
    }

    @Test
    void runner의_wakeUp은_sleeper의_wakeUp으로_위임된다() {
        FakeSleeper sleeper = new FakeSleeper();
        AdaptivePollingRunner runner = new AdaptivePollingRunner(newBackoff(), sleeper);

        runner.wakeUp();
        runner.wakeUp();

        assertThat(sleeper.wakeUpCount).isEqualTo(2);
    }

    private static class FakeSleeper implements Sleeper {
        private final List<Duration> requestedSleeps = new ArrayList<>();
        private int wakeUpCount = 0;

        @Override
        public void sleep(Duration duration) {
            requestedSleeps.add(duration);
        }

        @Override
        public void wakeUp() {
            wakeUpCount++;
        }
    }
}
