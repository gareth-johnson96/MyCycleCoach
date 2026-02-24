package com.mycyclecoach.infrastructure;

import com.mycyclecoach.feature.auth.exception.EmailNotVerifiedException;
import com.mycyclecoach.feature.auth.exception.InvalidCredentialsException;
import com.mycyclecoach.feature.auth.exception.InvalidVerificationTokenException;
import com.mycyclecoach.feature.auth.exception.TokenExpiredException;
import com.mycyclecoach.feature.auth.exception.UserAlreadyExistsException;
import com.mycyclecoach.feature.gpxanalysis.domain.GpxFileNotFoundException;
import com.mycyclecoach.feature.gpxanalysis.domain.GpxParsingException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return new ErrorResponse(400, "Bad Request", message, request.getRequestURI(), LocalDateTime.now());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUserAlreadyExists(UserAlreadyExistsException ex, HttpServletRequest request) {
        log.warn("User already exists: {}", ex.getMessage());
        return new ErrorResponse(400, "Bad Request", ex.getMessage(), request.getRequestURI(), LocalDateTime.now());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        log.warn("Invalid credentials: {}", ex.getMessage());
        return new ErrorResponse(400, "Bad Request", ex.getMessage(), request.getRequestURI(), LocalDateTime.now());
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleEmailNotVerified(EmailNotVerifiedException ex, HttpServletRequest request) {
        log.warn("Email not verified: {}", ex.getMessage());
        return new ErrorResponse(403, "Forbidden", ex.getMessage(), request.getRequestURI(), LocalDateTime.now());
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidVerificationToken(
            InvalidVerificationTokenException ex, HttpServletRequest request) {
        log.warn("Invalid verification token: {}", ex.getMessage());
        return new ErrorResponse(400, "Bad Request", ex.getMessage(), request.getRequestURI(), LocalDateTime.now());
    }

    @ExceptionHandler(TokenExpiredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTokenExpired(TokenExpiredException ex, HttpServletRequest request) {
        log.warn("Token expired: {}", ex.getMessage());
        return new ErrorResponse(400, "Bad Request", ex.getMessage(), request.getRequestURI(), LocalDateTime.now());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return new ErrorResponse(400, "Bad Request", ex.getMessage(), request.getRequestURI(), LocalDateTime.now());
    }

    @ExceptionHandler(GpxFileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleGpxFileNotFound(GpxFileNotFoundException ex, HttpServletRequest request) {
        log.warn("GPX file not found: {}", ex.getMessage());
        return new ErrorResponse(404, "Not Found", ex.getMessage(), request.getRequestURI(), LocalDateTime.now());
    }

    @ExceptionHandler(GpxParsingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleGpxParsing(GpxParsingException ex, HttpServletRequest request) {
        log.error("GPX parsing error: {}", ex.getMessage(), ex);
        return new ErrorResponse(400, "Bad Request", ex.getMessage(), request.getRequestURI(), LocalDateTime.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception", ex);
        return new ErrorResponse(
                500,
                "Internal Server Error",
                "An unexpected error occurred",
                request.getRequestURI(),
                LocalDateTime.now());
    }
}
