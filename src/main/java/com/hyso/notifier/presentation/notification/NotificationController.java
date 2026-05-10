package com.hyso.notifier.presentation.notification;

import com.hyso.notifier.application.notification.NotificationService;
import com.hyso.notifier.application.notification.RegisterNotificationResult;
import com.hyso.notifier.presentation.notification.dto.request.CreateNotificationRequest;
import com.hyso.notifier.presentation.notification.dto.response.CreateNotificationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
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
}
