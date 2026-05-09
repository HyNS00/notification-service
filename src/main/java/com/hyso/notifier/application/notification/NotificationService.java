package com.hyso.notifier.application.notification;

import com.hyso.notifier.domain.notification.Notification;
import com.hyso.notifier.domain.notification.repository.NotificationRepository;
import com.hyso.notifier.domain.notification.repository.NotificationSaveResult;
import com.hyso.notifier.presentation.notification.dto.request.CreateNotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final Clock clock;
    private final IdempotencyKeyGenerator idempotencyKeyGenerator;
    private final NotificationMessageRenderer notificationMessageRenderer;
    private final NotificationRepository notificationRepository;

    @Transactional
    public RegisterNotificationResult register(CreateNotificationRequest request) {
        String idempotencyKey = createIdempotencyKey(request);
        String body = notificationMessageRenderer.render(request.type());
        LocalDateTime now = LocalDateTime.now(clock);
        Notification notification = createNotification(request, body, idempotencyKey, now);

        NotificationSaveResult result = notificationRepository.saveOrFind(notification);
        return new RegisterNotificationResult(result.notification().getId(), result.created());
    }

    private String createIdempotencyKey(CreateNotificationRequest request) {
        return idempotencyKeyGenerator.generate(
                request.receiverId(),
                request.type(),
                request.refType(),
                request.refId(),
                request.channel()
        );
    }

    private Notification createNotification(
            CreateNotificationRequest request,
            String body,
            String idempotencyKey,
            LocalDateTime createdAt
    ) {
        return Notification.create(
                request.receiverId(),
                request.type(),
                request.channel(),
                request.refType(),
                request.refId(),
                body,
                idempotencyKey,
                createdAt
        );
    }
}
