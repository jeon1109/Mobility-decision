package com.example.musinsaPointSystem.data.decision;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.data.decision.model.DecisionRecommendation;
import com.example.musinsaPointSystem.data.decision.model.EvaluatedCandidate;
import com.example.musinsaPointSystem.data.decision.model.MobilityCandidate;
import com.example.musinsaPointSystem.data.decision.persistence.DecisionPersistenceService;
import com.example.musinsaPointSystem.data.decision.port.DecisionEvaluator;
import com.example.musinsaPointSystem.data.decision.port.MobilityCandidateProvider;
import com.example.musinsaPointSystem.dto.mobility.CurrentEvidenceResponse;
import com.example.musinsaPointSystem.dto.mobility.MobilityDecisionCaseRequest;
import com.example.musinsaPointSystem.dto.mobility.MobilityDecisionCaseResponse;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;

@Service
public class MobilityDecisionOrchestrator {
	private final CurrentEvidenceUseCase evidenceUseCase;
	private final MobilityCandidateProvider candidateProvider;
	private final CandidateEvaluator candidateEvaluator;
	private final DecisionEvaluator decisionEvaluator;
	private final DecisionPersistenceService persistenceService;
	private final MobilityPerformanceMetrics metrics;
	private final Clock clock;

	public MobilityDecisionOrchestrator(CurrentEvidenceUseCase evidenceUseCase,
		MobilityCandidateProvider candidateProvider, CandidateEvaluator candidateEvaluator,
		DecisionEvaluator decisionEvaluator, DecisionPersistenceService persistenceService,
		MobilityPerformanceMetrics metrics, Clock clock) {
		this.evidenceUseCase = evidenceUseCase; this.candidateProvider = candidateProvider;
		this.candidateEvaluator = candidateEvaluator; this.decisionEvaluator = decisionEvaluator;
		this.persistenceService = persistenceService; this.metrics = metrics; this.clock = clock;
	}

	public MobilityDecisionCaseResponse decide(MobilityDecisionCaseRequest request) {
		long startedAt = System.nanoTime();
		CurrentEvidenceResponse evidence = metrics.record("evidence.total",
			() -> evidenceUseCase.get(request.origin()));
		List<MobilityCandidate> candidates = metrics.record("candidate.collection.duration",
			() -> candidateProvider.findCandidates(request.origin(), request.destination()));
		List<EvaluatedCandidate> evaluated = metrics.record("candidate.evaluation.duration",
			() -> candidateEvaluator.evaluate(candidates, request.preferences(), evidence.city()));
		metrics.recordCandidateCount(evaluated.size());
		DecisionRecommendation recommendation = decisionEvaluator.evaluate(evidence.city(), evaluated,
			request.preferences());
		String decisionId = persistenceService.save(currentUser(), request.origin(), request.destination(),
			request.preferences(), evidence.city(), evaluated, recommendation);
		metrics.recordDuration("decision.total.duration", "success", System.nanoTime() - startedAt);
		return new MobilityDecisionCaseResponse("2.0", decisionId,
			OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).toString(), evidence,
			evaluated, recommendation);
	}

	private String currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated())
			throw new IllegalArgumentException("로그인한 사용자 정보가 필요합니다.");
		return authentication.getName();
	}
}
