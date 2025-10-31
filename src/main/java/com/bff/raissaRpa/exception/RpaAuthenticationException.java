package com.bff.raissaRpa.exception;

public class RpaAuthenticationException extends RuntimeException {
    public RpaAuthenticationException(String message) {
        super(message);
    }

    public RpaAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
