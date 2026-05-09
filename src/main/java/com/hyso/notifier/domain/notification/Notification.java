package com.hyso.notifier.domain.notification;

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
        name = "notifications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notifications_idem", columnNames = "idempotency_key")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(name = "ref_type", nullable = false, length = 64)
    private String refType;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(name = "body", nullable = false, length = 500)
    private String body;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Notification(
            Long receiverId,
            NotificationType type,
            NotificationChannel channel,
            String refType,
            Long refId,
            String body,
            String idempotencyKey,
            LocalDateTime createdAt
    ) {
        this.receiverId = receiverId;
        this.type = type;
        this.channel = channel;
        this.refType = refType;
        this.refId = refId;
        this.body = body;
        this.idempotencyKey = idempotencyKey;
        this.createdAt = createdAt;
    }

    public static Notification create(
            Long receiverId,
            NotificationType type,
            NotificationChannel channel,
            String refType,
            Long refId,
            String body,
            String idempotencyKey,
            LocalDateTime createdAt
    ) {
        validate(receiverId, type, channel, refType, refId, body, idempotencyKey, createdAt);
        return new Notification(receiverId, type, channel, refType, refId, body, idempotencyKey, createdAt);
    }

    private static void validate(
            Long receiverId,
            NotificationType type,
            NotificationChannel channel,
            String refType,
            Long refId,
            String body,
            String idempotencyKey,
            LocalDateTime createdAt
    ) {
        validateReceiverId(receiverId);
        validateType(type);
        validateChannel(channel);
        validateRefType(refType);
        validateRefId(refId);
        validateBody(body);
        validateIdempotencyKey(idempotencyKey);
        validateCreatedAt(createdAt);
    }

    private static void validateReceiverId(Long receiverId) {
        if (receiverId == null) {
            throw new IllegalArgumentException("수신자 ID 는 비어 있을 수 없습니다.");
        }
    }

    private static void validateType(NotificationType type) {
        if (type == null) {
            throw new IllegalArgumentException("알림 타입은 비어 있을 수 없습니다.");
        }
    }

    private static void validateChannel(NotificationChannel channel) {
        if (channel == null) {
            throw new IllegalArgumentException("발송 채널은 비어 있을 수 없습니다.");
        }
    }

    private static void validateRefType(String refType) {
        if (refType == null || refType.isBlank()) {
            throw new IllegalArgumentException("참조 타입은 비어 있을 수 없습니다.");
        }
        if (refType.length() > 64) {
            throw new IllegalArgumentException("참조 타입은 64자를 넘을 수 없습니다.");
        }
    }

    private static void validateRefId(Long refId) {
        if (refId == null) {
            throw new IllegalArgumentException("참조 ID 는 비어 있을 수 없습니다.");
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

    private static void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("멱등성 키는 비어 있을 수 없습니다.");
        }
        if (idempotencyKey.length() > 64) {
            throw new IllegalArgumentException("멱등성 키는 64자를 넘을 수 없습니다.");
        }
    }

    private static void validateCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) {
            throw new IllegalArgumentException("생성 시각은 비어 있을 수 없습니다.");
        }
    }
}
