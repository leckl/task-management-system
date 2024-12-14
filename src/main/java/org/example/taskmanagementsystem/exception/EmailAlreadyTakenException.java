package org.example.taskmanagementsystem.exception;

public class EmailAlreadyTakenException extends RuntimeException {
    public EmailAlreadyTakenException(String message) {
        super(message);
    }
}