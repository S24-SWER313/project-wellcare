package com.wellcare.wellcare.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceType, Long resourceId) {
        super("Resource of type '" + resourceType + "' with ID '" + resourceId + "' not found");
    }

    public ResourceNotFoundException(String resourceType, Long resourceId, Throwable cause) {
        super("Resource of type '" + resourceType + "' with ID '" + resourceId + "' not found", cause);
    }
}
