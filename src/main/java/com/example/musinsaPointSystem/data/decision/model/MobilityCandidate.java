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
	java.util.List<String> routeLinkIds,
	java.util.List<java.util.List<RoutePoint>> routeGeometry
) {
	public record RoutePoint(double latitude, double longitude) {}
	public MobilityCandidate {
		routeLinkIds = routeLinkIds == null ? java.util.List.of() : java.util.List.copyOf(routeLinkIds);
		routeGeometry = routeGeometry == null ? java.util.List.of()
			: routeGeometry.stream().map(java.util.List::copyOf).toList();
	}
	public MobilityCandidate(String candidateId, TransportType transportType, Location origin, Location destination,
		Integer estimatedDurationSeconds, Integer walkingDurationSeconds, Integer waitingDurationSeconds,
		Integer transfers, Integer estimatedCostWon, Reliability reliability, String routeSummary, String source,
		java.util.List<String> routeLinkIds) {
		this(candidateId, transportType, origin, destination, estimatedDurationSeconds, walkingDurationSeconds,
			waitingDurationSeconds, transfers, estimatedCostWon, reliability, routeSummary, source, routeLinkIds, java.util.List.of());
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
