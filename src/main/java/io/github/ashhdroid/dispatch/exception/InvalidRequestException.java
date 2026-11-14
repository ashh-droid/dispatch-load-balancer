package io.github.ashhdroid.dispatch.exception;

/** Invalid values for query parameters, independent of dispatch strategy selection. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) { super(message); }
}
