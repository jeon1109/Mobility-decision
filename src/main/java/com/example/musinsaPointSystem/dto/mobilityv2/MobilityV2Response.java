package com.example.musinsaPointSystem.dto.mobilityv2;

import java.util.List;

import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Request.TransportMode;

public record MobilityV2Response(
	String schemaVersion,
	String generatedAt,
	Area area,
	Guidance guidance,
	Situation situation,
	Forecasts forecasts,
	List<Alert> alerts,
	CitySituation.NearbyMobility nearbyMobility,
	List<String> priorities,
	String warning
) {
	public record Area(String code, String name) {}
	public record Guidance(String strategyCode, TransportMode recommendedMode, String headline,
		String summary, List<Evidence> evidence, List<Limitation> limitations) {}
	public record Evidence(String type, List<String> factIds, String message) {}
	public record Limitation(String code, String message) {}
	public record Situation(CitySituation.Congestion congestion, CitySituation.Weather weather,
		CitySituation.RoadTraffic roadTraffic, CitySituation.AirQuality airQuality) {}
	public record Forecasts(List<CitySituation.WeatherForecast> weather,
		List<CitySituation.CongestionForecast> congestion) {}
	public record Alert(String id, String type, String severity, String title, String message,
		String startsAt, String endsAt, String source, String status) {}
}
