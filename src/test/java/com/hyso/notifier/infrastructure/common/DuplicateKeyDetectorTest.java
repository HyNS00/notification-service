package com.hyso.notifier.infrastructure.common;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("NonAsciiCharacters")
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DuplicateKeyDetectorTest {

    private final DuplicateKeyDetector detector = new DuplicateKeyDetector();

    @Test
    void MySQL_duplicate_key_예외를_탐지한다() {
        SQLException exception = new SQLException("Duplicate entry", "23000", 1062);

        assertThat(detector.isDuplicateKey(exception)).isTrue();
    }

    @Test
    void H2_duplicate_key_예외를_탐지한다() {
        SQLException exception = new SQLException("Unique index or primary key violation", "23505", 23505);

        assertThat(detector.isDuplicateKey(exception)).isTrue();
    }

    @Test
    void 다른_제약_위반은_duplicate_key가_아니다() {
        SQLException exception = new SQLException("NULL not allowed", "23502", 23502);

        assertThat(detector.isDuplicateKey(exception)).isFalse();
    }

    @Test
    void Hibernate_ConstraintViolationException으로_감싸져도_duplicate_key를_탐지한다() {
        SQLException sqlException = new SQLException("Duplicate entry", "23000", 1062);
        ConstraintViolationException constraintViolationException =
                new ConstraintViolationException("duplicate key", sqlException, "uk_notifications_idem");
        RuntimeException wrapper = new RuntimeException(constraintViolationException);

        assertThat(detector.isDuplicateKey(wrapper)).isTrue();
    }

    @Test
    void null은_duplicate_key가_아니다() {
        assertThat(detector.isDuplicateKey(null)).isFalse();
    }
}
