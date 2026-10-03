package com.example.musinsaPointSystem.data.evidence.mobility;
import java.time.Instant;
import com.example.musinsaPointSystem.data.evidence.FreshnessStatus;
public record TrafficEvidence(String linkId, String roadName, Double speedKph, Double travelTimeSeconds,
    Double speed, Double travelTime, String speedUnit, String travelTimeUnit, TrafficStatus trafficStatus,
    Instant observedAt, Instant collectedAt, String source, FreshnessStatus freshness) {
    public enum TrafficStatus { FREE_FLOW, SLOW, CONGESTED, UNKNOWN }
}
