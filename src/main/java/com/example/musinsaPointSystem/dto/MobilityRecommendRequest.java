package com.example.musinsaPointSystem.dto;

public record MobilityRecommendRequest(
        String condition,
        String purpose,
        String areaName
) {
}
