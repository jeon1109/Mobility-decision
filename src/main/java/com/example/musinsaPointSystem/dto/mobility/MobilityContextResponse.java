package com.example.musinsaPointSystem.dto.mobility;

import java.util.List;

import com.example.musinsaPointSystem.data.evidence.FreshnessStatus;

public record MobilityContextResponse(
	String schemaVersion,
	String generatedAt,
	Origin origin,
	Situation situation,
	String warning
) {
	public record Origin(String areaCode, String areaName) {}

	public record Situation(
		WeatherEvidence weather,
		CongestionEvidence congestion,
		TrafficEvidence traffic,
		SubwayEvidence subway
	) {}

	public record WeatherEvidence(
		CitySituation.DataStatus status,
		Double temperatureCelsius,
		String condition,
		String precipitationType,
		Double humidityPercent,
		String observedAt,
		String source,
		String collectedAt,
		FreshnessStatus freshnessStatus
	) {}

	public record CongestionEvidence(
		CitySituation.DataStatus status,
		String level,
		String message,
		Integer populationMin,
		Integer populationMax,
		String observedAt,
		String source,
		String collectedAt,
		FreshnessStatus freshnessStatus
	) {}

	public record TrafficEvidence(
		CitySituation.DataStatus status,
		String level,
		Double averageSpeedKph,
		String message,
		String observedAt,
		String source,
		String collectedAt,
		FreshnessStatus freshnessStatus
	) {}

	public record SubwayEvidence(
		CitySituation.DataStatus status,
		Integer count,
		String observedAt,
		String source,
		String collectedAt,
		FreshnessStatus freshnessStatus,
		List<SubwayStation> stations
	) {}

	public record SubwayStation(String name) {}
}
