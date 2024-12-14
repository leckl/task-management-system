package org.example.taskmanagementsystem.exception;

public class AlreadyAuthenticatedException extends RuntimeException {
    public AlreadyAuthenticatedException(String message) {
        super(message);
    }
}