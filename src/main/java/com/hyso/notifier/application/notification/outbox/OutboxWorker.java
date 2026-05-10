package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.application.notification.outbox.poll.AdaptivePollingRunner;
import com.hyso.notifier.application.notification.outbox.poll.OutboxRowAvailableEvent;
import com.hyso.notifier.application.notification.outbox.poll.PollingHint;
import com.hyso.notifier.global.config.OutboxWorkerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker implements SmartLifecycle {

    private static final long STOP_JOIN_TIMEOUT_MS = 5_000L;
    private static final String WORKER_THREAD_NAME = "outbox-worker";

    private final AdaptivePollingRunner adaptivePollingRunner;
    private final OutboxProcessor outboxProcessor;
    private final OutboxWorkerProperties outboxWorkerProperties;

    private volatile boolean running;
    private volatile Thread workerThread;

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        Thread thread = new Thread(this::runLoop, WORKER_THREAD_NAME);
        thread.setDaemon(true);
        workerThread = thread;
        thread.start();
    }

    @Override
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        adaptivePollingRunner.wakeUp();
        Thread thread = workerThread;
        workerThread = null;
        if (thread != null) {
            try {
                thread.join(STOP_JOIN_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return outboxWorkerProperties.autoStart();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxRowAvailable(OutboxRowAvailableEvent event) {
        adaptivePollingRunner.wakeUp();
    }

    private void runLoop() {
        while (running) {
            try {
                adaptivePollingRunner.runOnce(this::runCycle);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("OutboxWorker cycle 실패", e);
            }
        }
    }

    private PollingHint runCycle() {
        List<ClaimedOutbox> claimed = outboxProcessor.claimBatch();
        if (claimed.isEmpty()) {
            return PollingHint.IDLE;
        }
        for (ClaimedOutbox c : claimed) {
            outboxProcessor.dispatchAndPersist(c);
        }
        return PollingHint.HAS_WORK;
    }
}
