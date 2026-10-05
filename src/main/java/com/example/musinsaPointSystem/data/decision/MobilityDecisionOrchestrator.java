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
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(MobilityDecisionOrchestrator.class);
	private final CurrentEvidenceUseCase evidenceUseCase;
	private final MobilityCandidateProvider candidateProvider;
	private final CandidateEvaluator candidateEvaluator;
	private final DecisionPersistenceService persistenceService;
	private final MobilityPerformanceMetrics metrics;
	private final Clock clock;
	private final com.example.musinsaPointSystem.data.location.SeoulServiceAreaPolicy serviceAreaPolicy;

	public MobilityDecisionOrchestrator(CurrentEvidenceUseCase evidenceUseCase,
		MobilityCandidateProvider candidateProvider, CandidateEvaluator candidateEvaluator,
		DecisionEvaluator decisionEvaluator, DecisionPersistenceService persistenceService,
		MobilityPerformanceMetrics metrics, Clock clock,
		com.example.musinsaPointSystem.data.location.SeoulServiceAreaPolicy serviceAreaPolicy) {
		this.evidenceUseCase = evidenceUseCase; this.candidateProvider = candidateProvider;
		this.candidateEvaluator = candidateEvaluator;
		this.persistenceService = persistenceService; this.metrics = metrics; this.clock = clock;
		this.serviceAreaPolicy = serviceAreaPolicy;
	}

	public MobilityDecisionCaseResponse decide(MobilityDecisionCaseRequest request) {
		return analyze(request, false);
	}

	public MobilityDecisionCaseResponse decideGuest(MobilityDecisionCaseRequest request) {
		com.example.musinsaPointSystem.data.decision.guest.GuestRequestPolicy.validate(request);
		return analyze(request, true);
	}

	private MobilityDecisionCaseResponse analyze(MobilityDecisionCaseRequest request, boolean guest) {
		long startedAt = System.nanoTime();
		var origin = serviceAreaPolicy.verify(request.origin());
		var destination = serviceAreaPolicy.verify(request.destination());
		String owner = guest ? null : currentUser();
		CurrentEvidenceResponse evidence = metrics.record("evidence.total",
			() -> evidenceUseCase.getVerified(origin));
		List<MobilityCandidate> candidates = metrics.record("candidate.collection.duration",
			() -> candidateProvider.findCandidates(origin, destination).stream().filter(candidate -> switch (request.modeCategory()) {
				case ANY -> true;
				case CAR -> candidate.transportType() == MobilityCandidate.TransportType.CAR;
				case PUBLIC_TRANSIT -> candidate.transportType() == MobilityCandidate.TransportType.BUS
					|| candidate.transportType() == MobilityCandidate.TransportType.SUBWAY
					|| candidate.transportType() == MobilityCandidate.TransportType.MIXED_TRANSIT;
			}).toList());
		List<EvaluatedCandidate> evaluated = metrics.record("candidate.evaluation.duration",
			() -> candidateEvaluator.evaluate(candidates, request.preferences(), evidence));
		metrics.recordCandidateCount(evaluated.size());
        log.info("[MOBILITY-ROUTE] candidates={} linkedCandidates={} trafficAvailability={} incidentAvailability={} policy=CONTROL_INCIDENT_CONGESTION_FIRST_WITH_DETOUR_LIMIT",
            candidates.size(),candidates.stream().filter(c->!c.routeLinkIds().isEmpty()).count(),evidence.trafficAvailability(),evidence.incidentAvailability());
		// Route decisions are rule-only; do not spend time or API quota on AI explanations.
		DecisionRecommendation recommendation = RuleRecommendationPolicy.recommend(evaluated);
        log.info("[MOBILITY-AI] result=SKIPPED reason=RULE_ONLY_DECISION");
		String decisionId = owner == null ? "guest-" + java.util.UUID.randomUUID()
			: persistenceService.save(owner, origin, destination, request.preferences(), evidence, evaluated, recommendation);
		metrics.recordDuration("decision.total.duration", "success", System.nanoTime() - startedAt);
		return new MobilityDecisionCaseResponse("2.0", decisionId,
			OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).toString(), evidence,
			evaluated, recommendation, owner == null ? "NOT_SAVED" : "SAVED");
	}

	private String currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
			|| authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)
			throw new IllegalArgumentException("로그인한 사용자 정보가 필요합니다.");
		return authentication.getName();
	}
}
