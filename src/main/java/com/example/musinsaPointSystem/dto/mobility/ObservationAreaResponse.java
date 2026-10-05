package com.example.musinsaPointSystem.dto.mobility;
import java.math.BigDecimal;
public record ObservationAreaResponse(String areaCode,String name,BigDecimal latitude,BigDecimal longitude,
    boolean coordinateVerified,String coordinateType,String coordinateSource,String geoStatus,
    String availability,String level,String message,String source,String observedAt,String freshness) {}
