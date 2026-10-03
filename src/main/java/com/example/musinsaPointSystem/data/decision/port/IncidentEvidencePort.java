package com.example.musinsaPointSystem.data.decision.port;
import com.example.musinsaPointSystem.data.evidence.mobility.*;
public interface IncidentEvidencePort {
    EvidenceBatch<IncidentEvidence> collectIncidents();
}
