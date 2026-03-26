package com.example.orcamento.exception;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            WebRequest request
    ) {
        logClientError(ex, HttpStatus.BAD_REQUEST, request);
        return createErrorResponse(
                "Requisicao invalida",
                "Corpo da requisicao ausente ou JSON invalido",
                HttpStatus.BAD_REQUEST,
                request,
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        logValidationError(ex, request, fieldErrors);
        return createErrorResponse(
                "Validacao invalida",
                "Um ou mais campos estao invalidos",
                HttpStatus.BAD_REQUEST,
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        logClientError(ex, HttpStatus.BAD_REQUEST, request);
        return createErrorResponse("BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST, request, null);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        logClientError(ex, HttpStatus.NOT_FOUND, request);
        return createErrorResponse("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND, request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        logClientError(ex, HttpStatus.NOT_FOUND, request);
        return createErrorResponse("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND, request, null);
    }

    @ExceptionHandler(MyFileNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleMyFileNotFound(MyFileNotFoundException ex, WebRequest request) {
        logClientError(ex, HttpStatus.NOT_FOUND, request);
        return createErrorResponse("FILE_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND, request, null);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleFileStorageException(FileStorageException ex, WebRequest request) {
        logServerError(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
        return createErrorResponse("FILE_STORAGE_ERROR", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        logServerError(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
        return createErrorResponse("INTERNAL_ERROR", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, request, null);
    }

    private void logClientError(Exception ex, HttpStatus status, WebRequest request) {
        RequestContext requestContext = extractRequestContext(request);
        LOGGER.warn(
                "request.error status={} method={} path={} exception={} message={}",
                status.value(),
                requestContext.method(),
                requestContext.path(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );
    }

    private void logServerError(Exception ex, HttpStatus status, WebRequest request) {
        RequestContext requestContext = extractRequestContext(request);
        LOGGER.error(
                "request.error status={} method={} path={} exception={} message={}",
                status.value(),
                requestContext.method(),
                requestContext.path(),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );
    }

    private void logValidationError(
            MethodArgumentNotValidException ex,
            WebRequest request,
            Map<String, String> validationErrors
    ) {
        RequestContext requestContext = extractRequestContext(request);
        LOGGER.warn(
                "request.error status={} method={} path={} exception={} validationErrors={}",
                HttpStatus.BAD_REQUEST.value(),
                requestContext.method(),
                requestContext.path(),
                ex.getClass().getSimpleName(),
                validationErrors
        );
    }

    private ResponseEntity<ApiErrorResponse> createErrorResponse(
            String error,
            String message,
            HttpStatus status,
            WebRequest request,
            Map<String, String> validationErrors
    ) {
        RequestContext requestContext = extractRequestContext(request);
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                OffsetDateTime.now().toString(),
                status.value(),
                error,
                message,
                requestContext.path(),
                MDC.get("traceId"),
                validationErrors != null && !validationErrors.isEmpty() ? validationErrors : null
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    private RequestContext extractRequestContext(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return new RequestContext(
                    servletWebRequest.getHttpMethod() != null ? servletWebRequest.getHttpMethod().name() : "UNKNOWN",
                    servletWebRequest.getRequest().getRequestURI()
            );
        }
        return new RequestContext("UNKNOWN", null);
    }

    private record RequestContext(String method, String path) {
    }
}
