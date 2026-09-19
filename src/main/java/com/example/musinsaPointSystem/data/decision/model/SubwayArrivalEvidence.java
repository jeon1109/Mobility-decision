package com.example.musinsaPointSystem.data.decision.model;

import com.example.musinsaPointSystem.data.evidence.FreshnessStatus;

public record SubwayArrivalEvidence(
	String stationName,
	String line,
	String direction,
	Integer arrivalSeconds,
	String arrivalMessage,
	String receivedAt,
	String observedAt,
	String source,
	FreshnessStatus freshness
) {}
