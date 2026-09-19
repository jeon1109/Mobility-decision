package com.example.musinsaPointSystem.dto.mobility;

import java.util.List;

import com.example.musinsaPointSystem.dto.mobility.MobilityRequest.TransportMode;

public record MobilityResponse(
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
	public enum StrategyCode {
		INSUFFICIENT_DATA,
		CONSIDER_WALK, CONSIDER_PUBLIC_TRANSIT, CONSIDER_BIKE,
		CONSIDER_CAR, CONSIDER_TAXI, CONSIDER_EV,
		CONSIDER_DELAYED_DEPARTURE, CAUTION_CROWD;

		public static StrategyCode consider(TransportMode mode) {
			return valueOf("CONSIDER_" + mode.name());
		}
	}

	public enum EvidenceType { OBSERVATION, USER_STATE, USER_PREFERENCE, SYSTEM_JUDGMENT }

	public enum LimitationCode {
		ROUTE_DATA_NOT_AVAILABLE, TRANSIT_AVAILABILITY_NOT_VERIFIED,
		CROWD_DATA_STALE, CROWD_DATA_UNAVAILABLE,
		TRAFFIC_DATA_STALE, TRAFFIC_DATA_UNAVAILABLE
	}

	public enum AlertSeverity { INFO, WARNING, CRITICAL }
	public enum AlertStatus { ACTIVE, EXPIRED }

	public record Area(String code, String name) {}
	public record Guidance(StrategyCode strategyCode, TransportMode recommendedMode, String headline,
		String summary, List<Evidence> evidence, List<Limitation> limitations) {}
	public record Evidence(EvidenceType type, List<String> factIds, String message) {}
	public record Limitation(LimitationCode code, String message) {}
	public record Situation(CitySituation.Congestion congestion, CitySituation.Weather weather,
		CitySituation.RoadTraffic roadTraffic, CitySituation.AirQuality airQuality) {}
	public record Forecasts(List<CitySituation.WeatherForecast> weather,
		List<CitySituation.CongestionForecast> congestion) {}
	public record Alert(String id, String type, AlertSeverity severity, String title, String message,
		String startsAt, String endsAt, String source, AlertStatus status) {}
}