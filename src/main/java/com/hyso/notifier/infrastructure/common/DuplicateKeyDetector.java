package com.hyso.notifier.infrastructure.common;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
public class DuplicateKeyDetector {

    public boolean isDuplicateKey(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException
                    && isDuplicateKey(constraintViolationException.getSQLException())) {
                return true;
            }
            if (current instanceof SQLException sqlException && isDuplicateKey(sqlException)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isDuplicateKey(SQLException sqlException) {
        if (sqlException == null) {
            return false;
        }
        if ("23000".equals(sqlException.getSQLState()) && sqlException.getErrorCode() == 1062) {
            return true;
        }
        return "23505".equals(sqlException.getSQLState());
    }
}
