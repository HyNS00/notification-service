package com.hyso.notifier.infrastructure.notification.persistence.outbox;

import com.hyso.notifier.domain.notification.outbox.NotificationOutbox;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface JpaNotificationOutboxRepository extends ListCrudRepository<NotificationOutbox, Long> {

    Optional<NotificationOutbox> findByIdempotencyKey(String idempotencyKey);
}
