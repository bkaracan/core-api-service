package com.enterprise.coreapi.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Kurumsal hata kodları kataloğu.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    VALIDATION_FAILED("CORE-4001", HttpStatus.BAD_REQUEST, "Validation criteria not satisfied"),
    RESOURCE_NOT_FOUND("CORE-4004", HttpStatus.NOT_FOUND, "Requested resource does not exist"),
    RESOURCE_ALREADY_EXISTS("CORE-4009", HttpStatus.CONFLICT, "Resource conflict detected"),
    UNAUTHORIZED_ACCESS("AUTH-4001", HttpStatus.UNAUTHORIZED, "Authentication credentials required or invalid"),
    ACCESS_DENIED("AUTH-4003", HttpStatus.FORBIDDEN, "Insufficient privileges to access this resource"),
    INTERNAL_SERVER_ERROR("CORE-5000", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected server error occurred");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
