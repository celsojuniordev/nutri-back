package com.br.nutri.common;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiError.FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR", "Dados inválidos.", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyInUse(EmailAlreadyInUseException ex) {
        ApiError error = new ApiError(HttpStatus.CONFLICT.value(), "EMAIL_ALREADY_IN_USE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ApiError error =
                new ApiError(HttpStatus.CONFLICT.value(), "EMAIL_ALREADY_IN_USE", "E-mail já está em uso.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException ex) {
        ApiError error = new ApiError(HttpStatus.UNAUTHORIZED.value(), "INVALID_CREDENTIALS", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(GoogleTokenInvalidException.class)
    public ResponseEntity<ApiError> handleGoogleTokenInvalid(GoogleTokenInvalidException ex) {
        ApiError error = new ApiError(HttpStatus.UNAUTHORIZED.value(), "GOOGLE_TOKEN_INVALID", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Autenticação necessária para acessar este recurso.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
}
