package com.hyso.notifier.application.notification.outbox.dispatcher;

import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailLogDispatcher implements NotificationDispatcher {

    @Override
    public void dispatch(NotificationOutbox outbox) {
        if (outbox == null) {
            throw new IllegalArgumentException("dispatch 대상 outbox 는 비어 있을 수 없습니다.");
        }
        log.info(
                "[EMAIL] outboxId={} receiver={} body={}",
                outbox.getId(),
                outbox.getReceiverId(),
                outbox.getBody()
        );
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }
}
