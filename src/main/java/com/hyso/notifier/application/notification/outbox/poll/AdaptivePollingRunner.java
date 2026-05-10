package com.hyso.notifier.application.notification.outbox.poll;

import java.util.function.Supplier;

public class AdaptivePollingRunner {

    private final Backoff backoff;
    private final Sleeper sleeper;

    public AdaptivePollingRunner(Backoff backoff, Sleeper sleeper) {
        validate(backoff, sleeper);
        this.backoff = backoff;
        this.sleeper = sleeper;
    }

    public void runOnce(Supplier<PollingHint> task) throws InterruptedException {
        if (task == null) {
            throw new IllegalArgumentException("폴링 task 는 비어 있을 수 없습니다.");
        }
        PollingHint hint = task.get();
        if (hint == null) {
            throw new IllegalArgumentException("PollingHint 는 비어 있을 수 없습니다.");
        }
        if (hint == PollingHint.HAS_WORK) {
            backoff.reset();
        }
        sleeper.sleep(backoff.next());
    }

    public void wakeUp() {
        sleeper.wakeUp();
    }

    private static void validate(Backoff backoff, Sleeper sleeper) {
        validateBackoff(backoff);
        validateSleeper(sleeper);
    }

    private static void validateBackoff(Backoff backoff) {
        if (backoff == null) {
            throw new IllegalArgumentException("Backoff 는 비어 있을 수 없습니다.");
        }
    }

    private static void validateSleeper(Sleeper sleeper) {
        if (sleeper == null) {
            throw new IllegalArgumentException("Sleeper 는 비어 있을 수 없습니다.");
        }
    }
}
