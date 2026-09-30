package com.enterprise.coreapi.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Kurumsal taban domain istisna sınıfı.
 */
@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public ApiException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
    }

    public ApiException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
    }
}
