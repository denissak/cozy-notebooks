package com.cozy.notebooks.exception;

/**
 * Thrown when an update is rejected because the caller's view of the resource
 * is stale (e.g. {@code baseHash} on a page update does not match the current
 * {@code content_hash}). Mapped to HTTP 409 by GlobalExceptionHandler.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
