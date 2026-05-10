package com.hyso.notifier.infrastructure.notification.persistence;

import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.repository.NotificationRepository;
import com.hyso.notifier.domain.notification.repository.NotificationSaveResult;
import com.hyso.notifier.infrastructure.common.DuplicateKeyDetector;
import com.hyso.notifier.infrastructure.notification.exception.OrphanedDuplicateException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final JpaNotificationRepository jpaNotificationRepository;
    private final NotificationCreator notificationCreator;
    private final DuplicateKeyDetector duplicateKeyDetector;

    @Override
    public Notification save(Notification notification) {
        return jpaNotificationRepository.save(notification);
    }

    @Override
    public NotificationSaveResult saveOrFind(Notification notification) {
        try {
            return NotificationSaveResult.created(notificationCreator.saveNew(notification));
        } catch (DataIntegrityViolationException exception) {
            if (duplicateKeyDetector.isDuplicateKey(exception)) {
                return NotificationSaveResult.existing(findExisting(notification));
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> findByIdempotencyKey(String idempotencyKey) {
        return jpaNotificationRepository.findByIdempotencyKey(idempotencyKey);
    }

    private Notification findExisting(Notification notification) {
        return jpaNotificationRepository.findByIdempotencyKey(notification.getIdempotencyKey())
                .orElseThrow(() -> new OrphanedDuplicateException(notification.getIdempotencyKey()));
    }
}
