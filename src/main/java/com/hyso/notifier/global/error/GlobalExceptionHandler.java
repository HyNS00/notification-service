package com.hyso.notifier.global.error;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException exception) {
        return createResponseEntity(CommonErrorCode.INVALID_INPUT, exception.getMessage());
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
