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
	String source
) {
	public enum TransportType { WALK, BUS, SUBWAY, MIXED_TRANSIT, CAR }
	public enum Reliability { HIGH, MEDIUM, LOW, UNKNOWN }
}
