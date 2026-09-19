package com.example.musinsaPointSystem.data.decision.model;

import java.math.BigDecimal;

public record NearbyStation(
	String stationId,
	String stationName,
	String line,
	Integer distanceMeters,
	Integer walkingDurationSeconds,
	BigDecimal latitude,
	BigDecimal longitude,
	String source
) {}
