package com.hyso.notifier.application.notification.outbox.poll;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class WaitingSleeper implements Sleeper {

    private final Semaphore permits = new Semaphore(0);

    @Override
    public void sleep(Duration duration) throws InterruptedException {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return;
        }
        permits.tryAcquire(duration.toNanos(), TimeUnit.NANOSECONDS);
        permits.drainPermits();
    }

    @Override
    public void wakeUp() {
        permits.release();
    }
}
