package com.example.musinsaPointSystem.data.decision.model;

import com.example.musinsaPointSystem.data.location.model.Location;

public record MobilityCandidate(
	String candidateId,
	TransportType transportType,
	Location origin,
	Location destination,
	Integer estimatedDurationSeconds,
	Integer walkingDurationSeconds,
	Integer waitingDurationSeconds,
	Integer transfers,
	Integer estimatedCostWon,
	Reliability reliability,
	String routeSummary,
	String source,
	java.util.List<String> routeLinkIds
) {
	public MobilityCandidate {
		routeLinkIds = routeLinkIds == null ? java.util.List.of() : java.util.List.copyOf(routeLinkIds);
	}
	public MobilityCandidate(String candidateId, TransportType transportType, Location origin, Location destination,
		Integer estimatedDurationSeconds, Integer walkingDurationSeconds, Integer waitingDurationSeconds,
		Integer transfers, Integer estimatedCostWon, Reliability reliability, String routeSummary, String source) {
		this(candidateId, transportType, origin, destination, estimatedDurationSeconds, walkingDurationSeconds,
			waitingDurationSeconds, transfers, estimatedCostWon, reliability, routeSummary, source, java.util.List.of());
	}
	public enum TransportType { WALK, BUS, SUBWAY, MIXED_TRANSIT, CAR }
	public enum Reliability { HIGH, MEDIUM, LOW, UNKNOWN }
}
