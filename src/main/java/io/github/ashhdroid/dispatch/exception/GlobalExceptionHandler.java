package io.github.ashhdroid.dispatch.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.github.ashhdroid.dispatch.dto.ErrorResponse;
import io.github.ashhdroid.dispatch.dto.ErrorResponse.FieldError;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldError::field)).toList();
        return handleExceptionInternal(ex, new ErrorResponse("error", "Validation failed.", errors),
                headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof InvalidFormatException ife) {
                String field = fieldPath(ife.getPath());
                String message = Number.class.isAssignableFrom(ife.getTargetType())
                        ? "must be a number" : "has an invalid value";
                return handleExceptionInternal(ex, new ErrorResponse("error", "Validation failed.",
                        List.of(new FieldError(field, message))), headers, HttpStatus.BAD_REQUEST, request);
            }
            if (cause.getMessage() != null && cause.getMessage().contains("priority must be one of")) {
                return handleExceptionInternal(ex, ErrorResponse.of("priority must be one of HIGH, MEDIUM, LOW"),
                        headers, HttpStatus.BAD_REQUEST, request);
            }
            cause = cause.getCause();
        }
        return handleExceptionInternal(ex, ErrorResponse.of("Request body is not valid JSON."),
                headers, HttpStatus.BAD_REQUEST, request);
    }

    private static String fieldPath(List<JsonMappingException.Reference> path) {
        StringBuilder result = new StringBuilder();
        for (var ref : path) {
            if (ref.getFieldName() != null) {
                if (!result.isEmpty()) result.append('.');
                result.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) result.append('[').append(ref.getIndex()).append(']');
        }
        return result.toString();
    }

    @Override protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (!(body instanceof ErrorResponse)) {
            String message = switch (status.value()) {
                case 404 -> "Resource not found.";
                case 405 -> "HTTP method not allowed.";
                case 406 -> "Requested response media type is not available.";
                case 415 -> "Unsupported request media type; use application/json.";
                default -> status.is4xxClientError() ? "Invalid request." : "Something went wrong.";
            };
            body = ErrorResponse.of(message);
        }
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    @ExceptionHandler({DuplicateIdException.class, InvalidStrategyException.class, InvalidRequestException.class})
    public ResponseEntity<ErrorResponse> invalid(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(ex.getMessage()));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> wrongQueryType(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse("error", "Validation failed.",
                List.of(new FieldError(ex.getName(), "has an invalid type"))));
    }
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> invalidParameter(ConstraintViolationException ex) {
        var errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldError(v.getPropertyPath().toString().replaceFirst("^.*\\.", ""), v.getMessage()))
                .sorted(Comparator.comparing(FieldError::field)).toList();
        return ResponseEntity.badRequest().body(new ErrorResponse("error", "Validation failed.", errors));
    }
    @ExceptionHandler(DeleteDisabledException.class)
    public ResponseEntity<ErrorResponse> deleteDisabled(DeleteDisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of(ex.getMessage()));
    }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(ex.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception ex) {
        log.error("Unhandled backend exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of("Something went wrong."));
    }
}
