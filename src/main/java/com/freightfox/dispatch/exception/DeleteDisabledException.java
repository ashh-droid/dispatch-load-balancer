package com.freightfox.dispatch.exception;

/** Raised when demo data mutation endpoints are disabled on a hosted instance. */
public class DeleteDisabledException extends RuntimeException {
    public DeleteDisabledException() {
        super("Delete endpoints are disabled on this deployment.");
    }
}
