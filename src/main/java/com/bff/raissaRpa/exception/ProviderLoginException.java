package com.bff.raissaRpa.exception;

public class ProviderLoginException extends RuntimeException {
    public ProviderLoginException(String message) {
        super(message);
    }

    public ProviderLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
