package com.enterprise.coreapi.common.exception;

/**
 * Kaynak bulunamadığında fırlatılan 404 istisnası.
 */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "%s with %s '%s' not found".formatted(resourceName, fieldName, fieldValue));
    }
}
