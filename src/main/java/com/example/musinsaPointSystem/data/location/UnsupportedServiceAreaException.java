package com.example.musinsaPointSystem.data.location;

public class UnsupportedServiceAreaException extends RuntimeException {
    public UnsupportedServiceAreaException() { super("현재 서울 지역만 지원합니다. 장소의 행정구역을 확인해주세요."); }
}
