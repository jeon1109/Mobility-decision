package com.example.musinsaPointSystem.data.apiController;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.musinsaPointSystem.data.apiService.MobilityContextUseCase;
import com.example.musinsaPointSystem.data.apiService.MobilityRecommendationService;
import com.example.musinsaPointSystem.data.apiService.SeoulAreaService;
import com.example.musinsaPointSystem.data.decision.CurrentEvidenceUseCase;
import com.example.musinsaPointSystem.data.decision.MobilityDecisionOrchestrator;
import com.example.musinsaPointSystem.data.decision.persistence.DecisionPersistenceService;
import com.example.musinsaPointSystem.data.location.model.Location;
import com.example.musinsaPointSystem.dto.SeoulArea;
import com.example.musinsaPointSystem.dto.mobility.MobilityContextResponse;
import com.example.musinsaPointSystem.dto.mobility.MobilityFlowRequest;
import com.example.musinsaPointSystem.dto.mobility.MobilityRequest;
import com.example.musinsaPointSystem.dto.mobility.MobilityResponse;
import com.example.musinsaPointSystem.dto.mobility.CurrentEvidenceResponse;
import com.example.musinsaPointSystem.dto.mobility.MobilityDecisionCaseRequest;
import com.example.musinsaPointSystem.dto.mobility.MobilityDecisionCaseResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
@RestController
@RequestMapping("/api/v1")
public class MobilityRestController {
	private final SeoulAreaService seoulAreaService;
	private final MobilityRecommendationService recommendationService;
	private final MobilityContextUseCase contextUseCase;
	private final CurrentEvidenceUseCase currentEvidenceUseCase;
	private final MobilityDecisionOrchestrator decisionOrchestrator;
	private final DecisionPersistenceService decisionPersistenceService;

	@Autowired
	public MobilityRestController(SeoulAreaService seoulAreaService,
		MobilityRecommendationService recommendationService, MobilityContextUseCase contextUseCase,
		CurrentEvidenceUseCase currentEvidenceUseCase, MobilityDecisionOrchestrator decisionOrchestrator,
		DecisionPersistenceService decisionPersistenceService) {
		this.seoulAreaService = seoulAreaService;
		this.recommendationService = recommendationService;
		this.contextUseCase = contextUseCase;
		this.currentEvidenceUseCase = currentEvidenceUseCase;
		this.decisionOrchestrator = decisionOrchestrator;
		this.decisionPersistenceService = decisionPersistenceService;
	}

	public MobilityRestController(SeoulAreaService seoulAreaService,
		MobilityRecommendationService recommendationService, MobilityContextUseCase contextUseCase) {
		this(seoulAreaService, recommendationService, contextUseCase, null, null, null);
	}

	@GetMapping(value = "/areas", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<SeoulArea> getAreas() {
		return seoulAreaService.getAreas();
	}

	@GetMapping(value = "/mobility/context", produces = MediaType.APPLICATION_JSON_VALUE)
	public MobilityContextResponse getMobilityContext(
		@RequestParam(name = "originAreaCode") @NotBlank String originAreaCode
	) {
		return contextUseCase.getCurrentContext(originAreaCode);
	}

	@PostMapping(value = "/mobility/context", consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE)
	public CurrentEvidenceResponse getMobilityContext(@RequestBody Location origin) {
		return currentEvidenceUseCase.get(origin);
	}

	@PostMapping(value = "/mobility/decision-cases", consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE)
	public MobilityDecisionCaseResponse createDecisionCase(
		@Valid @RequestBody MobilityDecisionCaseRequest request) {
		return decisionOrchestrator.decide(request);
	}

	@PostMapping(value = "/mobility/decision-cases/{decisionId}/selection",
		consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public DecisionPersistenceService.SelectionResult selectCandidate(
		@PathVariable(name = "decisionId") String decisionId,
		@Valid @RequestBody CandidateSelectionRequest request,
		java.security.Principal principal) {
		return decisionPersistenceService.select(decisionId, request.candidateId(), principal.getName());
	}

	public record CandidateSelectionRequest(@NotBlank String candidateId) {}

	@PostMapping(value = "/mobility/decisions", consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MobilityResponse> createFlowDecision(
		@Valid @RequestBody MobilityFlowRequest request
	) {
		return ResponseEntity.ok(recommendationService.recommend(request));
	}

	@PostMapping(value = "/mobility-decisions", consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<MobilityResponse> createMobilityDecision(
		@Valid @RequestBody MobilityRequest request
	) {
		return ResponseEntity.ok(recommendationService.recommend(request));
	}
}
