package com.hyso.notifier.application.notification.outbox.poll;

import java.time.Duration;

public interface Sleeper {

    void sleep(Duration duration) throws InterruptedException;

    void wakeUp();
}
