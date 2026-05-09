package com.hyso.notifier.infrastructure.notification.persistence;

import com.hyso.notifier.domain.notification.Notification;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface JpaNotificationRepository extends ListCrudRepository<Notification, Long> {

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);
}
