package io.github.ashhdroid.dispatch.dto;

import java.util.List;

public record ErrorResponse(String status, String message, List<FieldError> errors) {
    public record FieldError(String field, String message) {}
    public static ErrorResponse of(String message) { return new ErrorResponse("error", message, List.of()); }
}
