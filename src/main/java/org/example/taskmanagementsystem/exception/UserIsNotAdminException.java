package org.example.taskmanagementsystem.exception;

public class UserIsNotAdminException extends RuntimeException {
    public UserIsNotAdminException(String message) {
        super(message);
    }
}