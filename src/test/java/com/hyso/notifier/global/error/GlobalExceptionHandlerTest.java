package com.hyso.notifier.global.error;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void IllegalArgumentException은_INVALID_INPUT으로_응답한다() {
        ResponseEntity<Object> actual =
                handler.handleIllegalArgumentException(new IllegalArgumentException("참조 ID 는 비어 있을 수 없습니다."));

        assertAll(
                () -> assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(actual.getBody()).isEqualTo(
                        new ExceptionResponse("INVALID_INPUT", "참조 ID 는 비어 있을 수 없습니다.")
                )
        );
    }

    @Test
    void IllegalStateException은_INTERNAL_ERROR로_응답한다() {
        ResponseEntity<Object> actual = handler.handleIllegalStateException();

        assertAll(
                () -> assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
                () -> assertThat(actual.getBody()).isEqualTo(
                        new ExceptionResponse("INTERNAL_ERROR", "요청을 처리할 수 없습니다.")
                )
        );
    }

    @Test
    void OrphanedDuplicateException은_ORPHANED_DUPLICATE로_응답한다() {
        ResponseEntity<Object> actual = handler.handleOrphanedDuplicateException();

        assertAll(
                () -> assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
                () -> assertThat(actual.getBody()).isEqualTo(
                        new ExceptionResponse("ORPHANED_DUPLICATE", "알림 등록 처리에 실패했습니다.")
                )
        );
    }

    @Test
    void 알_수_없는_예외는_INTERNAL_ERROR로_응답한다() {
        ResponseEntity<Object> actual = handler.handleException();

        assertAll(
                () -> assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
                () -> assertThat(actual.getBody()).isEqualTo(
                        new ExceptionResponse("INTERNAL_ERROR", "요청을 처리할 수 없습니다.")
                )
        );
    }
}
