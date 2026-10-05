package com.example.musinsaPointSystem.data.decision.guest;
public class GuestAdmissionException extends RuntimeException {
    private final int status;private final String code;private final int retryAfter;
    public GuestAdmissionException(int status,String code,String message,int retryAfter){
        super(message);this.status=status;this.code=code;this.retryAfter=retryAfter;
    }
    public int status(){return status;}public String code(){return code;}public int retryAfter(){return retryAfter;}
}
