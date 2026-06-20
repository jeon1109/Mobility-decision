package com.example.musinsaPointSystem.common.error;

public class AiCommunicationException extends RuntimeException {
    public AiCommunicationException(String message) {
        super(message);
    }

    public AiCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
