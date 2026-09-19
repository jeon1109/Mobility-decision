package com.example.musinsaPointSystem.dto.mobility;

import java.util.List;

import com.example.musinsaPointSystem.data.decision.model.DecisionRecommendation;
import com.example.musinsaPointSystem.data.decision.model.EvaluatedCandidate;

public record MobilityDecisionCaseResponse(
	String schemaVersion,
	String decisionId,
	String generatedAt,
	CurrentEvidenceResponse currentEvidence,
	List<EvaluatedCandidate> candidates,
	DecisionRecommendation recommendation
) {
	public MobilityDecisionCaseResponse {
		candidates = candidates == null ? List.of() : List.copyOf(candidates);
	}
}
