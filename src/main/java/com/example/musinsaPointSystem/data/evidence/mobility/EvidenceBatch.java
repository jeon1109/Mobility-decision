package com.example.musinsaPointSystem.data.evidence.mobility;
import java.util.List;
public record EvidenceBatch<T>(EvidenceAvailability availability, List<T> items) {
    public EvidenceBatch { items = items == null ? List.of() : List.copyOf(items); }
    public static <T> EvidenceBatch<T> unavailable() { return new EvidenceBatch<>(EvidenceAvailability.UNAVAILABLE, List.of()); }
}
