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

import java.time.LocalDateTime;
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
    @Transactional
    public void markSent(Long notificationId, LocalDateTime sentAt) {
        validateMarkSent(notificationId, sentAt);
        jpaNotificationRepository.markSent(notificationId, sentAt);
    }

    @Override
    @Transactional
    public void markFailed(Long notificationId, LocalDateTime failedAt, String failureReason) {
        validateMarkFailed(notificationId, failedAt, failureReason);
        jpaNotificationRepository.markFailed(notificationId, failedAt, failureReason);
    }

    private Notification findExisting(Notification notification) {
        return jpaNotificationRepository.findByIdempotencyKey(notification.getIdempotencyKey())
                .orElseThrow(() -> new OrphanedDuplicateException(notification.getIdempotencyKey()));
    }

    private static void validateMarkSent(Long notificationId, LocalDateTime sentAt) {
        validateNotificationId(notificationId);
        validateSentAt(sentAt);
    }

    private static void validateMarkFailed(Long notificationId, LocalDateTime failedAt, String failureReason) {
        validateNotificationId(notificationId);
        validateFailedAt(failedAt);
        validateFailureReason(failureReason);
    }

    private static void validateNotificationId(Long notificationId) {
        if (notificationId == null) {
            throw new IllegalArgumentException("알림 ID 는 비어 있을 수 없습니다.");
        }
    }

    private static void validateSentAt(LocalDateTime sentAt) {
        if (sentAt == null) {
            throw new IllegalArgumentException("발송 완료 시각은 비어 있을 수 없습니다.");
        }
    }

    private static void validateFailedAt(LocalDateTime failedAt) {
        if (failedAt == null) {
            throw new IllegalArgumentException("실패 시각은 비어 있을 수 없습니다.");
        }
    }

    private static void validateFailureReason(String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("실패 사유는 비어 있을 수 없습니다.");
        }
    }
}
