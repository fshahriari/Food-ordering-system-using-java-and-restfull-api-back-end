package com.snappfood.exception;

/**
 * Custom exception to represent an HTTP 409 Conflict error.
 * This is typically used when a request cannot be completed due to a
 * conflict with the current state of the target resource, such as an
 * edit conflict between multiple simultaneous updates.
 */
public class ConflictException extends Exception {
    public ConflictException(String message) {
        super(message);
    }
}
