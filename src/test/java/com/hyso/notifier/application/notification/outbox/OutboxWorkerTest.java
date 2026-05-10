package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.application.notification.outbox.poll.AdaptivePollingRunner;
import com.hyso.notifier.application.notification.outbox.poll.OutboxRowAvailableEvent;
import com.hyso.notifier.global.config.OutboxWorkerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@SuppressWarnings("NonAsciiCharacters")
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OutboxWorkerTest {

    @Mock
    AdaptivePollingRunner adaptivePollingRunner;
    @Mock
    OutboxProcessor outboxProcessor;
    @Mock
    OutboxWorkerProperties outboxWorkerProperties;

    OutboxWorker worker;

    @BeforeEach
    void setUp() throws InterruptedException {
        worker = new OutboxWorker(adaptivePollingRunner, outboxProcessor, outboxWorkerProperties);
        // runOnce 가 즉시 반환되면 워커 thread 가 CPU 를 폭주하므로 짧은 sleep 으로 throttle.
        lenient().doAnswer(invocation -> {
            Thread.sleep(10);
            return null;
        }).when(adaptivePollingRunner).runOnce(any());
    }

    @Test
    void onOutboxRowAvailable_은_AdaptivePollingRunner_의_wakeUp_을_호출한다() {
        worker.onOutboxRowAvailable(new OutboxRowAvailableEvent());

        verify(adaptivePollingRunner).wakeUp();
    }

    @Test
    void start_과_stop_은_정상적으로_생애주기를_관리한다() {
        worker.start();
        assertThat(worker.isRunning()).isTrue();

        worker.stop();

        assertThat(worker.isRunning()).isFalse();
        verify(adaptivePollingRunner, atLeastOnce()).wakeUp();
    }

    @Test
    void 이미_실행_중인_상태에서_start_를_다시_호출해도_무시된다() {
        worker.start();
        try {
            worker.start();
            assertThat(worker.isRunning()).isTrue();
        } finally {
            worker.stop();
        }
    }
}
