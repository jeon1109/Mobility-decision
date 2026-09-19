package com.example.musinsaPointSystem.data.decision.model;

import java.util.List;

public record DecisionRecommendation(
	String recommendedCandidateId,
	String summary,
	List<String> reasons,
	List<Alternative> alternatives,
	List<String> risks,
	Double confidence,
	boolean aiAvailable
) {
	public DecisionRecommendation {
		reasons = reasons == null ? List.of() : List.copyOf(reasons);
		alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
		risks = risks == null ? List.of() : List.copyOf(risks);
	}

	public static DecisionRecommendation unavailable() {
		return new DecisionRecommendation(null,
			"AI 분석을 현재 사용할 수 없습니다. 실제 이동 후보를 직접 비교해주세요.",
			List.of(), List.of(), List.of(), null, false);
	}

	public record Alternative(String candidateId, String reason) {}
}
