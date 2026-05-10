package com.hyso.notifier.application.notification.outbox.dispatcher;

import com.hyso.notifier.domain.notification.NotificationChannel;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;

public interface NotificationDispatcher {

    void dispatch(NotificationOutbox outbox);

    NotificationChannel getChannel();
}
