package com.hyso.notifier.application.notification.outbox.poll;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PollingHintEmitterTest {

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    @Test
    void emit_을_호출하면_OutboxRowAvailableEvent_가_publish_된다() {
        PollingHintEmitter emitter = new PollingHintEmitter(applicationEventPublisher);

        emitter.emit();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(OutboxRowAvailableEvent.class);
    }
}
