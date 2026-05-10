package com.hyso.notifier.global.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String getErrorCode();

    String getMessage();

    HttpStatus getHttpStatus();
}
