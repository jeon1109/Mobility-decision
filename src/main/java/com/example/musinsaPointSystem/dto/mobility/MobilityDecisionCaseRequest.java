package com.example.musinsaPointSystem.dto.mobility;

import java.util.List;

import com.example.musinsaPointSystem.data.decision.model.DecisionPreference;
import com.example.musinsaPointSystem.data.location.model.Location;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MobilityDecisionCaseRequest(
	@NotNull Location origin,
	@NotNull Location destination,
	@NotNull MobilityRequest.Purpose purpose,
	@NotNull @Size(min = 1, max = 5) List<DecisionPreference> preferences
) {
	public MobilityDecisionCaseRequest {
		preferences = preferences == null ? null : preferences.stream().distinct().toList();
	}
}
