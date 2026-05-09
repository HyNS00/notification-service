package com.hyso.notifier.domain.notification.outbox;

import com.hyso.notifier.domain.notification.NotificationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification_outboxes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_outboxes_notification", columnNames = "notification_id"),
                @UniqueConstraint(name = "uk_notification_outboxes_idem", columnNames = "idempotency_key")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationOutboxStatus status;

    @Column(name = "processing_attempt", nullable = false)
    private int processingAttempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_lease_state", nullable = false, length = 16)
    private NotificationOutboxLeaseState processingLeaseState;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(name = "body", nullable = false, length = 500)
    private String body;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private NotificationOutbox(
            Long notificationId,
            String idempotencyKey,
            Long receiverId,
            NotificationChannel channel,
            String body,
            LocalDateTime createdAt
    ) {
        this.notificationId = notificationId;
        this.idempotencyKey = idempotencyKey;
        this.receiverId = receiverId;
        this.channel = channel;
        this.body = body;
        this.status = NotificationOutboxStatus.PENDING;
        this.processingAttempt = 0;
        this.processingLeaseState = NotificationOutboxLeaseState.IDLE;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static NotificationOutbox create(
            Long notificationId,
            String idempotencyKey,
            Long receiverId,
            NotificationChannel channel,
            String body,
            LocalDateTime createdAt
    ) {
        validate(notificationId, idempotencyKey, receiverId, channel, body, createdAt);
        return new NotificationOutbox(notificationId, idempotencyKey, receiverId, channel, body, createdAt);
    }

    private static void validate(
            Long notificationId,
            String idempotencyKey,
            Long receiverId,
            NotificationChannel channel,
            String body,
            LocalDateTime createdAt
    ) {
        validateNotificationId(notificationId);
        validateIdempotencyKey(idempotencyKey);
        validateReceiverId(receiverId);
        validateChannel(channel);
        validateBody(body);
        validateCreatedAt(createdAt);
    }

    private static void validateNotificationId(Long notificationId) {
        if (notificationId == null) {
            throw new IllegalArgumentException("알림 ID 는 비어 있을 수 없습니다.");
        }
    }

    private static void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("멱등성 키는 비어 있을 수 없습니다.");
        }
        if (idempotencyKey.length() > 64) {
            throw new IllegalArgumentException("멱등성 키는 64자를 넘을 수 없습니다.");
        }
    }

    private static void validateReceiverId(Long receiverId) {
        if (receiverId == null) {
            throw new IllegalArgumentException("수신자 ID 는 비어 있을 수 없습니다.");
        }
    }

    private static void validateChannel(NotificationChannel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("발송 채널은 비어 있을 수 없습니다.");
        }
    }

    private static void validateBody(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("알림 본문은 비어 있을 수 없습니다.");
        }
        if (body.length() > 500) {
            throw new IllegalArgumentException("알림 본문은 500자를 넘을 수 없습니다.");
        }
    }

    private static void validateCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("생성 시각은 비어 있을 수 없습니다.");
        }
    }
}
