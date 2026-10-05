package com.example.musinsaPointSystem.dto.mobility;

import java.util.List;

import com.example.musinsaPointSystem.data.decision.model.DecisionPreference;
import com.example.musinsaPointSystem.data.location.model.Location;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MobilityDecisionCaseRequest(
	@NotNull Location origin,
	@NotNull Location destination,
	MobilityRequest.Purpose purpose,
	@Size(max = 5) List<@NotNull DecisionPreference> preferences,
	ModeCategory modeCategory
) {
	public enum ModeCategory { PUBLIC_TRANSIT, CAR, ANY }
	public MobilityDecisionCaseRequest(Location origin, Location destination, MobilityRequest.Purpose purpose,
		List<DecisionPreference> preferences) { this(origin, destination, purpose, preferences, ModeCategory.ANY); }
	public MobilityDecisionCaseRequest {
		preferences = preferences == null ? List.of() : preferences.stream().distinct().toList();
		purpose = purpose == null ? MobilityRequest.Purpose.OTHER : purpose;
		modeCategory = modeCategory == null ? ModeCategory.ANY : modeCategory;
	}
}
