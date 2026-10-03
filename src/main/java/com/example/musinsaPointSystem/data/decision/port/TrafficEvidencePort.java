package com.example.musinsaPointSystem.data.decision.port;
import java.util.List;
import com.example.musinsaPointSystem.data.evidence.mobility.*;
public interface TrafficEvidencePort {
    EvidenceBatch<TrafficEvidence> findByLinks(List<String> linkIds);
}
