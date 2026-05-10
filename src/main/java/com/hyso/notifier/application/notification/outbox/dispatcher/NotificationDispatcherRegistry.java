package com.hyso.notifier.application.notification.outbox.dispatcher;

import com.hyso.notifier.application.notification.outbox.exception.UnsupportedDispatchChannelException;
import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationDispatcherRegistry {

    private final Map<NotificationChannel, NotificationDispatcher> dispatchers;

    public NotificationDispatcherRegistry(List<NotificationDispatcher> dispatchers) {
        if (dispatchers == null) {
            throw new IllegalArgumentException("dispatcher 목록은 비어 있을 수 없습니다.");
        }
        this.dispatchers = buildMap(dispatchers);
    }

    public void dispatch(NotificationChannel channel, NotificationOutbox outbox) {
        validate(channel, outbox);
        NotificationDispatcher dispatcher = dispatchers.get(channel);
        if (dispatcher == null) {
            throw new UnsupportedDispatchChannelException(channel);
        }
        dispatcher.dispatch(outbox);
    }

    private static Map<NotificationChannel, NotificationDispatcher> buildMap(
            List<NotificationDispatcher> dispatchers
    ) {
        Map<NotificationChannel, NotificationDispatcher> map = new EnumMap<>(NotificationChannel.class);
        for (NotificationDispatcher dispatcher : dispatchers) {
            NotificationDispatcher previous = map.put(dispatcher.getChannel(), dispatcher);
            if (previous != null) {
                throw new IllegalStateException(
                        "같은 채널에 둘 이상의 dispatcher 가 등록되었습니다: " + dispatcher.getChannel()
                );
            }
        }
        return Map.copyOf(map);
    }

    private static void validate(NotificationChannel channel, NotificationOutbox outbox) {
        validateChannel(channel);
        validateOutbox(outbox);
    }

    private static void validateChannel(NotificationChannel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("발송 채널은 비어 있을 수 없습니다.");
        }
    }

    private static void validateOutbox(NotificationOutbox outbox) {
        if (outbox == null) {
            throw new IllegalArgumentException("dispatch 대상 outbox 는 비어 있을 수 없습니다.");
        }
    }
}
