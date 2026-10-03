package com.example.musinsaPointSystem.data.decision.infrastructure.topis;
/** Never stores response body, request URI or cause: provider URI contains a credential. */
public class TopisException extends RuntimeException {
    private final boolean retryable;
    public TopisException(boolean retryable){super("TOPIS request unavailable");this.retryable=retryable;}
    public boolean retryable(){return retryable;}
}
