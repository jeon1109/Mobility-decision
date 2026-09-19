package com.example.musinsaPointSystem.data.decision.model;

public record CatchableTrain(
	String stationName,
	String line,
	String direction,
	Integer arrivalSeconds,
	boolean catchable,
	String reason
) {}
