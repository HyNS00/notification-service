package com.hyso.notifier.presentation.notification;

import com.hyso.notifier.application.notification.NotificationService;
import com.hyso.notifier.application.notification.RegisterNotificationResult;
import com.hyso.notifier.presentation.notification.dto.request.CreateNotificationRequest;
import com.hyso.notifier.presentation.notification.dto.response.CreateNotificationResponse;
import com.hyso.notifier.presentation.notification.dto.response.NotificationListResponse;
import com.hyso.notifier.presentation.notification.dto.response.NotificationResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<CreateNotificationResponse> create(@Valid @RequestBody CreateNotificationRequest request) {
        RegisterNotificationResult result = notificationService.register(request);
        HttpStatus status = result.created() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity
                .status(status)
                .body(new CreateNotificationResponse(result.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> findOne(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(notificationService.findOne(userId, id));
    }

    @GetMapping
    public ResponseEntity<NotificationListResponse> findList(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "limit 은 1 이상이어야 합니다.")
            @Max(value = 100, message = "limit 은 100 을 넘을 수 없습니다.")
            int limit
    ) {
        return ResponseEntity.ok(notificationService.findList(userId, read, limit));
    }
}
