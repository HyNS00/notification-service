package com.hyso.notifier.infrastructure.notification.persistence;

import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.repository.NotificationRepository;
import com.hyso.notifier.domain.notification.repository.NotificationSaveResult;
import com.hyso.notifier.domain.notification.repository.ReadFilter;
import com.hyso.notifier.infrastructure.common.DuplicateKeyDetector;
import com.hyso.notifier.infrastructure.notification.exception.OrphanedDuplicateException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    @Transactional(readOnly = true)
    public Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId) {
        return jpaNotificationRepository.findByIdAndReceiverId(id, receiverId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findPage(Long receiverId, ReadFilter filter, int limit) {
        return jpaNotificationRepository.findPage(
                receiverId,
                filter.name(),
                PageRequest.of(0, limit)
        );
    }

    @Override
    @Transactional
    public void markSent(Long notificationId, LocalDateTime sentAt) {
        jpaNotificationRepository.markSent(notificationId, sentAt);
    }

    @Override
    @Transactional
    public void markFailed(Long notificationId, LocalDateTime failedAt, String failureReason) {
        jpaNotificationRepository.markFailed(notificationId, failedAt, failureReason);
    }

    private Notification findExisting(Notification notification) {
        return jpaNotificationRepository.findByIdempotencyKey(notification.getIdempotencyKey())
                .orElseThrow(() -> new OrphanedDuplicateException(notification.getIdempotencyKey()));
    }
}
