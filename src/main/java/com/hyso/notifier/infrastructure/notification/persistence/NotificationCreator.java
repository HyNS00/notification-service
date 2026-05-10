package com.hyso.notifier.infrastructure.notification.persistence;

import com.hyso.notifier.application.notification.outbox.poll.PollingHintEmitter;
import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.infrastructure.notification.persistence.outbox.JpaNotificationOutboxRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class NotificationCreator {

    private final EntityManager entityManager;
    private final JpaNotificationRepository jpaNotificationRepository;
    private final JpaNotificationOutboxRepository jpaNotificationOutboxRepository;
    private final PollingHintEmitter pollingHintEmitter;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notification saveNew(Notification notification) {
        Notification saved = jpaNotificationRepository.save(notification);
        NotificationOutbox outbox = createOutbox(saved);
        jpaNotificationOutboxRepository.save(outbox);
        entityManager.flush();
        pollingHintEmitter.emit();
        return saved;
    }

    private NotificationOutbox createOutbox(Notification notification) {
        return NotificationOutbox.create(
                notification.getId(),
                notification.getIdempotencyKey(),
                notification.getReceiverId(),
                notification.getChannel(),
                notification.getBody(),
                notification.getCreatedAt()
        );
    }
}
