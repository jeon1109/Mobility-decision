package com.example.musinsaPointSystem.data.decision.infrastructure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import com.example.musinsaPointSystem.data.decision.model.DecisionPreference;
import com.example.musinsaPointSystem.data.decision.model.DecisionRecommendation;
import com.example.musinsaPointSystem.data.decision.model.EvaluatedCandidate;
import com.example.musinsaPointSystem.data.decision.port.DecisionEvaluator;
import com.example.musinsaPointSystem.dto.mobility.CitySituation;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SpringAiDecisionEvaluator implements DecisionEvaluator {
	private static final Logger log = LoggerFactory.getLogger(SpringAiDecisionEvaluator.class);
	private final ChatClient chatClient;
	private final ObjectMapper objectMapper;
	private final MobilityPerformanceMetrics metrics;

	public SpringAiDecisionEvaluator(ChatClient.Builder builder, ObjectMapper objectMapper,
		MobilityPerformanceMetrics metrics) {
		this.chatClient = builder.build();
		this.objectMapper = objectMapper;
		this.metrics = metrics;
	}

	@Override
	public DecisionRecommendation evaluate(CitySituation evidence, List<EvaluatedCandidate> candidates,
		List<DecisionPreference> preferences) {
		if (candidates == null || candidates.isEmpty()) return DecisionRecommendation.unavailable();
		try {
			ChatResponse response = metrics.record("spring.ai.duration", () -> chatClient.prompt()
				.system("""
					당신은 이동 후보 비교 설명기입니다. 경로를 생성하거나 시간·비용·노선·실시간 상태를
					추측하지 마세요. 제공된 candidateId만 추천·대안에 사용할 수 있습니다.
					반드시 recommendedCandidateId, summary, reasons, alternatives, risks, confidence 필드의
					JSON 객체만 출력하세요. confidence는 0~1 숫자입니다.
					""")
				.user(compactInput(evidence, candidates, preferences)).call().chatResponse());
			Usage usage = response.getMetadata().getUsage();
			if (usage != null) metrics.recordAiTokens(usage.getPromptTokens(), usage.getCompletionTokens());
			String content = response.getResult().getOutput().getText();
			DecisionRecommendation parsed = objectMapper.readValue(content, DecisionRecommendation.class);
			validate(parsed, candidates);
			return new DecisionRecommendation(parsed.recommendedCandidateId(), parsed.summary(), parsed.reasons(),
				parsed.alternatives(), parsed.risks(), parsed.confidence(), true);
		} catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException e) {
			metrics.recordAiValidationFailure();
			log.warn("Structured AI decision unavailable; candidates are preserved, errorType={}",
				e.getClass().getSimpleName());
			return DecisionRecommendation.unavailable();
		}
	}

	private String compactInput(CitySituation evidence, List<EvaluatedCandidate> candidates,
		List<DecisionPreference> preferences) {
		List<Object> compactCandidates = candidates.stream().map(value -> java.util.Map.of(
			"candidateId", value.candidate().candidateId(),
			"type", value.candidate().transportType(),
			"durationSeconds", nullable(value.candidate().estimatedDurationSeconds()),
			"walkingSeconds", nullable(value.candidate().walkingDurationSeconds()),
			"transfers", nullable(value.candidate().transfers()),
			"costWon", nullable(value.candidate().estimatedCostWon()),
			"score", value.score(), "insights", value.insights(), "risks", value.risks()
		)).map(Object.class::cast).toList();
		try {
			return objectMapper.writeValueAsString(java.util.Map.of(
				"weather", java.util.Map.of("status", evidence.weather().status(),
					"condition", nullable(evidence.weather().condition()),
					"precipitation", nullable(evidence.weather().precipitationType())),
				"traffic", java.util.Map.of("status", evidence.roadTraffic().status(),
					"level", nullable(evidence.roadTraffic().level())),
				"congestion", java.util.Map.of("status", evidence.congestion().status(),
					"level", nullable(evidence.congestion().level())),
				"preferences", preferences, "candidates", compactCandidates));
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			throw new IllegalStateException("Failed to build compact AI input", e);
		}
	}

	private Object nullable(Object value) { return value == null ? "UNKNOWN" : value; }

	void validate(DecisionRecommendation value, List<EvaluatedCandidate> candidates) {
		if (value == null || value.recommendedCandidateId() == null || value.summary() == null
			|| value.confidence() == null || value.confidence() < 0 || value.confidence() > 1) {
			throw new IllegalArgumentException("Invalid AI decision shape");
		}
		Set<String> ids = new HashSet<>();
		candidates.forEach(candidate -> ids.add(candidate.candidate().candidateId()));
		if (!ids.contains(value.recommendedCandidateId()))
			throw new IllegalArgumentException("AI recommended unknown candidate");
		if (value.alternatives().stream().anyMatch(alternative -> !ids.contains(alternative.candidateId())))
			throw new IllegalArgumentException("AI returned unknown alternative");
	}
}
