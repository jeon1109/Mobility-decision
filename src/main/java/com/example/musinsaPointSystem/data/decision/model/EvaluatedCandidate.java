package com.example.musinsaPointSystem.data.decision.model;

import java.util.List;

public record EvaluatedCandidate(
	MobilityCandidate candidate,
	double score,
	List<String> insights,
	List<String> risks,
	List<DecisionPreference> matchedPreferences
) {
	public EvaluatedCandidate {
		insights = insights == null ? List.of() : List.copyOf(insights);
		risks = risks == null ? List.of() : List.copyOf(risks);
		matchedPreferences = matchedPreferences == null ? List.of() : List.copyOf(matchedPreferences);
	}
}
