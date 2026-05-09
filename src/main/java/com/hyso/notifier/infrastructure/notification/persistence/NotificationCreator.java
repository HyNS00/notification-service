package com.hyso.notifier.infrastructure.notification.persistence;

import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import com.hyso.notifier.domain.notification.outbox.repository.NotificationOutboxRepository;
import com.hyso.notifier.domain.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class NotificationCreator {

    private final EntityManager entityManager;
    private final NotificationRepository notificationRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long saveNew(Notification notification, LocalDateTime createdAt) {
        Notification saved = notificationRepository.save(notification);
        NotificationOutbox outbox = createOutbox(saved, createdAt);
        notificationOutboxRepository.save(outbox);
        entityManager.flush();
        return saved.getId();
    }

    private NotificationOutbox createOutbox(Notification notification, LocalDateTime createdAt) {
        return NotificationOutbox.create(
                notification.getId(),
                notification.getIdempotencyKey(),
                notification.getReceiverId(),
                notification.getChannel(),
                notification.getBody(),
                createdAt
        );
    }
}
