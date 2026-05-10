package com.hyso.notifier.application.notification.outbox;

import com.hyso.notifier.application.notification.outbox.exception.TransientFailureException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RetryExceptionClassifierTest {

    private final RetryExceptionClassifier classifier = new RetryExceptionClassifier();

    @Test
    void TransientFailureException은_재시도_가능이다() {
        Throwable cause = new TransientFailureException("일시 장애");
        assertThat(classifier.classify(cause)).isEqualTo(RetryExceptionClassifier.Classification.RETRYABLE);
    }

    @Test
    void IOException은_재시도_가능이다() {
        Throwable cause = new IOException("네트워크 오류");
        assertThat(classifier.classify(cause)).isEqualTo(RetryExceptionClassifier.Classification.RETRYABLE);
    }

    @Test
    void IOException_하위_타입도_재시도_가능이다() {
        Throwable cause = new SocketTimeoutException("read timed out");
        assertThat(classifier.classify(cause)).isEqualTo(RetryExceptionClassifier.Classification.RETRYABLE);
    }

    @Test
    void TimeoutException은_재시도_가능이다() {
        Throwable cause = new TimeoutException("응답 지연");
        assertThat(classifier.classify(cause)).isEqualTo(RetryExceptionClassifier.Classification.RETRYABLE);
    }

    @Test
    void IllegalArgumentException은_재시도_불가이다() {
        Throwable cause = new IllegalArgumentException("잘못된 입력");
        assertThat(classifier.classify(cause)).isEqualTo(RetryExceptionClassifier.Classification.NON_RETRYABLE);
    }

    @Test
    void IllegalStateException은_재시도_불가이다() {
        Throwable cause = new IllegalStateException("내부 invariant 위반");
        assertThat(classifier.classify(cause)).isEqualTo(RetryExceptionClassifier.Classification.NON_RETRYABLE);
    }

    @Test
    void 분류되지_않은_RuntimeException은_재시도_불가로_본다() {
        Throwable cause = new RuntimeException("정의 안 된 예외");
        assertThat(classifier.classify(cause)).isEqualTo(RetryExceptionClassifier.Classification.NON_RETRYABLE);
    }

    @Test
    void cause가_null이면_IllegalArgumentException을_던진다() {
        assertThatThrownBy(() -> classifier.classify(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예외 원인은 비어 있을 수 없습니다.");
    }
}
