package com.example.musinsaPointSystem.data.evidence.mobility;
import java.time.Instant;
import java.util.List;
import com.example.musinsaPointSystem.data.evidence.FreshnessStatus;
public record IncidentEvidence(String id, IncidentCategory category, IncidentStatus status,
    String description, String linkId, String roadName, String affectedArea, Double latitude, Double longitude,
    Instant startedAt, Instant expectedEndAt, Instant observedAt, Instant collectedAt, String source,
    FreshnessStatus freshness, List<MobilityImpactType> impacts, String relevance) {
    public IncidentEvidence { impacts = impacts == null ? List.of() : List.copyOf(impacts); }
    @com.fasterxml.jackson.annotation.JsonProperty(value="coordinateVerified", access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public boolean coordinateVerified() {
        return "TOPIS".equals(source) && latitude != null && longitude != null && Double.isFinite(latitude) && Double.isFinite(longitude)
            && latitude >= 37.4 && latitude <= 37.75 && longitude >= 126.7 && longitude <= 127.25;
    }
    @com.fasterxml.jackson.annotation.JsonProperty(value="coordinateSource", access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public String coordinateSource() { return coordinateVerified() ? "TOPIS_EPSG_5181_CONVERTED" : null; }
    @com.fasterxml.jackson.annotation.JsonProperty(value="coordinateCrs", access=com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public String coordinateCrs() { return coordinateVerified() ? "EPSG:4326" : null; }
    public enum IncidentCategory { ACCIDENT, ROAD_CONTROL, CONSTRUCTION, PROTEST, FIRE, WEATHER, FAILURE, OTHER }
    public enum IncidentStatus { SCHEDULED, ACTIVE, RESOLVED, UNKNOWN }
    public enum MobilityImpactType { ROAD_CONTROL, TRAVEL_TIME_VARIABILITY, UNKNOWN }
}
