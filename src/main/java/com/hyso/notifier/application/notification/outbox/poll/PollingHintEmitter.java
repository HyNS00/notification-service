package com.hyso.notifier.application.notification.outbox.poll;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PollingHintEmitter {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void emit() {
        applicationEventPublisher.publishEvent(new OutboxRowAvailableEvent());
    }
}
