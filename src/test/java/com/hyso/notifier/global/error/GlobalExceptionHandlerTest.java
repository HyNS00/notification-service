package com.hyso.notifier.global.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
    void UnsupportedDispatchChannelException은_UNSUPPORTED_DISPATCH_CHANNEL로_응답한다() {
        ResponseEntity<Object> actual = handler.handleUnsupportedDispatchChannelException();

        assertAll(
                () -> assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
                () -> assertThat(actual.getBody()).isEqualTo(
                        new ExceptionResponse("UNSUPPORTED_DISPATCH_CHANNEL", "발송 채널을 처리할 수 없습니다.")
                )
        );
    }

    @Test
    void NotificationNotFoundException은_NOTIFICATION_NOT_FOUND로_응답한다() {
        ResponseEntity<Object> actual = handler.handleNotificationNotFoundException();

        assertAll(
                () -> assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                () -> assertThat(actual.getBody()).isEqualTo(
                        new ExceptionResponse("NOTIFICATION_NOT_FOUND", "알림을 찾을 수 없습니다.")
                )
        );
    }

    @Test
    void X_User_Id_헤더가_누락되면_사용자_식별_헤더_메시지로_응답한다() {
        MissingRequestHeaderException exception = mock(MissingRequestHeaderException.class);
        given(exception.getHeaderName()).willReturn("X-User-Id");

        ResponseEntity<Object> actual = handler.handleMissingRequestHeaderException(exception);

        assertAll(
                () -> assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(actual.getBody()).isEqualTo(
                        new ExceptionResponse("INVALID_INPUT", "사용자 식별 헤더가 비어 있을 수 없습니다.")
                )
        );
    }

    @Test
    void X_User_Id_외_헤더가_누락되면_일반_헤더_누락_메시지로_응답한다() {
        MissingRequestHeaderException exception = mock(MissingRequestHeaderException.class);
        given(exception.getHeaderName()).willReturn("X-Other-Header");

        ResponseEntity<Object> actual = handler.handleMissingRequestHeaderException(exception);

        assertAll(
                () -> assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(actual.getBody()).isEqualTo(
                        new ExceptionResponse("INVALID_INPUT", "필수 헤더가 누락되었습니다.")
                )
        );
    }

    @Test
    void 요청_값_형식이_맞지_않으면_형식_오류_메시지로_응답한다() {
        ResponseEntity<Object> actual = handler.handleMethodArgumentTypeMismatchException();

        assertAll(
                () -> assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(actual.getBody()).isEqualTo(
                        new ExceptionResponse("INVALID_INPUT", "요청 값의 형식이 올바르지 않습니다.")
                )
        );
    }

    @Test
    void ConstraintViolationException은_첫_위반_메시지로_INVALID_INPUT을_반환한다() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        given(violation.getMessage()).willReturn("limit 은 1 이상이어야 합니다.");
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<Object> actual = handler.handleConstraintViolationException(exception);

        assertAll(
                () -> assertThat(actual.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                () -> assertThat(actual.getBody()).isEqualTo(
                        new ExceptionResponse("INVALID_INPUT", "limit 은 1 이상이어야 합니다.")
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
