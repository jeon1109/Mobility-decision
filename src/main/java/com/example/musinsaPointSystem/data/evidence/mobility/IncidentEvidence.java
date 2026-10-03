package com.example.musinsaPointSystem.data.evidence.mobility;
import java.time.Instant;
import java.util.List;
import com.example.musinsaPointSystem.data.evidence.FreshnessStatus;
public record IncidentEvidence(String id, IncidentCategory category, IncidentStatus status,
    String description, String linkId, String roadName, String affectedArea, Double latitude, Double longitude,
    Instant startedAt, Instant expectedEndAt, Instant observedAt, Instant collectedAt, String source,
    FreshnessStatus freshness, List<MobilityImpactType> impacts, String relevance) {
    public IncidentEvidence { impacts = impacts == null ? List.of() : List.copyOf(impacts); }
    public enum IncidentCategory { ACCIDENT, ROAD_CONTROL, CONSTRUCTION, PROTEST, FIRE, WEATHER, FAILURE, OTHER }
    public enum IncidentStatus { SCHEDULED, ACTIVE, RESOLVED, UNKNOWN }
    public enum MobilityImpactType { ROAD_CONTROL, TRAVEL_TIME_VARIABILITY, UNKNOWN }
}
