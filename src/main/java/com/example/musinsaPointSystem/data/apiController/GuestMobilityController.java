package com.example.musinsaPointSystem.data.apiController;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.musinsaPointSystem.data.decision.MobilityDecisionOrchestrator;
import com.example.musinsaPointSystem.dto.mobility.MobilityDecisionCaseRequest;
import com.example.musinsaPointSystem.dto.mobility.MobilityDecisionCaseResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/mobility")
public class GuestMobilityController {
	private final MobilityDecisionOrchestrator orchestrator;

	public GuestMobilityController(MobilityDecisionOrchestrator orchestrator) {
		this.orchestrator = orchestrator;
	}

	@PostMapping(value = "/guest-decision-cases", consumes = "application/json", produces = "application/json")
	public MobilityDecisionCaseResponse analyze(@Valid @RequestBody MobilityDecisionCaseRequest request) {
		return orchestrator.decideGuest(request);
	}
}
