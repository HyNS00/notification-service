package com.hyso.notifier.global.error;

import com.hyso.notifier.application.notification.outbox.exception.UnsupportedDispatchChannelException;
import com.hyso.notifier.infrastructure.notification.exception.NotificationNotFoundException;
import com.hyso.notifier.infrastructure.notification.exception.OrphanedDuplicateException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ExceptionResponse response = ExceptionResponse.from(
                CommonErrorCode.INVALID_INPUT,
                firstFieldErrorMessage(exception)
        );

        return handleExceptionInternal(
                exception,
                response,
                headers,
                CommonErrorCode.INVALID_INPUT.getHttpStatus(),
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ExceptionResponse response = ExceptionResponse.from(
                CommonErrorCode.INVALID_INPUT,
                "요청 본문을 해석할 수 없습니다."
        );

        return handleExceptionInternal(
                exception,
                response,
                headers,
                CommonErrorCode.INVALID_INPUT.getHttpStatus(),
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException exception) {
        return createResponseEntity(CommonErrorCode.INVALID_INPUT, exception.getMessage());
    }

    @ExceptionHandler(OrphanedDuplicateException.class)
    public ResponseEntity<Object> handleOrphanedDuplicateException() {
        return createResponseEntity(NotificationErrorCode.ORPHANED_DUPLICATE);
    }

    @ExceptionHandler(UnsupportedDispatchChannelException.class)
    public ResponseEntity<Object> handleUnsupportedDispatchChannelException() {
        return createResponseEntity(NotificationErrorCode.UNSUPPORTED_DISPATCH_CHANNEL);
    }

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<Object> handleNotificationNotFoundException() {
        return createResponseEntity(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Object> handleMissingRequestHeaderException(MissingRequestHeaderException exception) {
        String message = "X-User-Id".equalsIgnoreCase(exception.getHeaderName())
                ? "사용자 식별 헤더가 비어 있을 수 없습니다."
                : "필수 헤더가 누락되었습니다.";
        return createResponseEntity(CommonErrorCode.INVALID_INPUT, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleMethodArgumentTypeMismatchException() {
        return createResponseEntity(CommonErrorCode.INVALID_INPUT, "요청 값의 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Object> handleIllegalStateException() {
        return createResponseEntity(CommonErrorCode.INTERNAL_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleException() {
        return createResponseEntity(CommonErrorCode.INTERNAL_ERROR);
    }

    private String firstFieldErrorMessage(MethodArgumentNotValidException exception) {
        return exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(CommonErrorCode.INVALID_INPUT.getMessage());
    }

    private ResponseEntity<Object> createResponseEntity(ErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ExceptionResponse.from(errorCode));
    }

    private ResponseEntity<Object> createResponseEntity(ErrorCode errorCode, String message) {
        String resolvedMessage = errorCode.getMessage();
        if (StringUtils.hasText(message)) {
            resolvedMessage = message;
        }

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ExceptionResponse.from(errorCode, resolvedMessage));
    }
}
