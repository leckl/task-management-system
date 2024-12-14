package org.example.taskmanagementsystem.controllers;

import org.example.taskmanagementsystem.exception.*;
import org.example.taskmanagementsystem.util.ApiMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskCreationException.class)
    public ResponseEntity<ApiMessageResponse> handleTaskCreationException(TaskCreationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiMessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiMessageResponse> handleUserNotFoundException(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<?> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiMessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiMessageResponse> handleTaskNotFoundException(TaskNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ApiMessageResponse> handleCommentNotFoundException(CommentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiMessageResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiMessageResponse("Страница не найдена"));
    }

    @ExceptionHandler(UserIsNotAdminException.class)
    public ResponseEntity<ApiMessageResponse> handleUserIsNotAdminException(UserIsNotAdminException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiMessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(AlreadyAuthenticatedException.class)
    public ResponseEntity<ApiMessageResponse> handleAlreadyAuthenticatedException(AlreadyAuthenticatedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiMessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiMessageResponse> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiMessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(EmailAlreadyTakenException.class)
    public ResponseEntity<ApiMessageResponse> handleEmailAlreadyTakenException(EmailAlreadyTakenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiMessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiMessageResponse("У вас нет прав на выполнение данной операции"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Произошла ошибка: " + ex);
    }
}
