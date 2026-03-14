// src/main/java/com/example/orcamento/exception/GlobalExceptionHandler.java
package com.example.orcamento.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import jakarta.persistence.EntityNotFoundException;

import java.util.HashMap;
import java.util.Map;
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        LOGGER.warn("HttpMessageNotReadableException: {}", ex.getMessage(), ex);
        return createErrorResponse(
                "Requisição inválida",
                "Corpo da requisição ausente ou JSON inválido",
                HttpStatus.BAD_REQUEST,
                request,
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        LOGGER.warn("MethodArgumentNotValidException: {}", ex.getMessage(), ex);
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return createErrorResponse(
                "Validação inválida",
                "Um ou mais campos estão inválidos",
                HttpStatus.BAD_REQUEST,
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        LOGGER.warn("IllegalArgumentException: {}", ex.getMessage(), ex);
        return createErrorResponse("Requisição inválida", ex.getMessage(), HttpStatus.BAD_REQUEST, request, null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        LOGGER.warn("EntityNotFoundException: {}", ex.getMessage(), ex);
        return createErrorResponse("Recurso não encontrado", ex.getMessage(), HttpStatus.NOT_FOUND, request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex, WebRequest request) {
        LOGGER.error("Exception occurred: ", ex);
        return createErrorResponse("Erro interno do servidor", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, request, null);
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(
            String error,
            String message,
            HttpStatus status,
            WebRequest request,
            Map<String, String> validationErrors
    ) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", error);
        errorResponse.put("message", message);
        errorResponse.put("status", status.value());
        errorResponse.put("timestamp", java.time.OffsetDateTime.now().toString());

        if (request instanceof ServletWebRequest servletWebRequest) {
            errorResponse.put("path", servletWebRequest.getRequest().getRequestURI());
        }
        if (validationErrors != null && !validationErrors.isEmpty()) {
            errorResponse.put("validationErrors", validationErrors);
        }

        return new ResponseEntity<>(errorResponse, status);
    }
}