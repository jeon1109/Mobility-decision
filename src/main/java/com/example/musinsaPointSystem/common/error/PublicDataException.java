package com.example.musinsaPointSystem.common.error;

public class PublicDataException extends RuntimeException {
    public PublicDataException(String message) {
        super(message);
    }

    public PublicDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
