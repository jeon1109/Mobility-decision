package com.example.musinsaPointSystem.data.decision.port;

import java.util.List;

import com.example.musinsaPointSystem.data.decision.model.DecisionPreference;
import com.example.musinsaPointSystem.data.decision.model.DecisionRecommendation;
import com.example.musinsaPointSystem.data.decision.model.EvaluatedCandidate;
import com.example.musinsaPointSystem.dto.mobility.CitySituation;

public interface DecisionEvaluator {
	DecisionRecommendation evaluate(CitySituation evidence, List<EvaluatedCandidate> candidates,
		List<DecisionPreference> preferences);
}
