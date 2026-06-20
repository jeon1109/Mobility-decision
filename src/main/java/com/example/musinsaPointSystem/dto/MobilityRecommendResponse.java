package com.example.musinsaPointSystem.dto;

public record MobilityRecommendResponse(
        String requestKey,
        MobilityDecision decision
) {
}
