package com.example.musinsaPointSystem.dto.mobilityv2;

import java.util.List;

public record CitySituation(
	String areaCode,
	String areaName,
	Congestion congestion,
	Weather weather,
	RoadTraffic roadTraffic,
	AirQuality airQuality,
	List<WeatherForecast> weatherForecasts,
	List<CongestionForecast> congestionForecasts,
	NearbyMobility nearbyMobility
) {
	public static final String SOURCE = "SEOUL_CITY_DATA";

	public enum DataStatus { LIVE, STALE, UNAVAILABLE }

	public record Congestion(DataStatus status, String level, String message, Integer populationMin,
		Integer populationMax, String observedAt, String source) {
		public static Congestion unavailable() {
			return new Congestion(DataStatus.UNAVAILABLE, null, null, null, null, null, SOURCE);
		}
	}

	public record Weather(DataStatus status, Double temperatureCelsius, String condition,
		String precipitationType, Double humidityPercent, String observedAt, String source) {
		public static Weather unavailable() {
			return new Weather(DataStatus.UNAVAILABLE, null, null, null, null, null, SOURCE);
		}
	}

	public record RoadTraffic(DataStatus status, String level, Double averageSpeedKph, String message,
		String observedAt, String source) {
		public static RoadTraffic unavailable() {
			return new RoadTraffic(DataStatus.UNAVAILABLE, null, null, null, null, SOURCE);
		}
	}

	public record AirQuality(DataStatus status, Double pm10, String pm10Level, Double pm25,
		String pm25Level, String observedAt, String source) {
	}

	public record WeatherForecast(String forecastAt, String condition, String precipitationType,
		Double rainChancePercent, Double temperatureCelsius, String source) {
	}

	public record CongestionForecast(String forecastAt, String level, Integer populationMin,
		Integer populationMax, String source) {
	}

	public record NearbySnapshot(DataStatus status, Integer count, String observedAt, String source) {
		public static NearbySnapshot unavailable() {
			return new NearbySnapshot(DataStatus.UNAVAILABLE, null, null, SOURCE);
		}
	}

	public record NearbyMobility(NearbySnapshot subwayStations, NearbySnapshot busStops,
		NearbySnapshot bikeStations, NearbySnapshot parkingLots, NearbySnapshot evChargingStations) {
		public static NearbyMobility unavailable() {
			NearbySnapshot value = NearbySnapshot.unavailable();
			return new NearbyMobility(value, value, value, value, value);
		}
	}

	public static CitySituation unavailable(String areaCode, String areaName) {
		return new CitySituation(areaCode, areaName, Congestion.unavailable(), Weather.unavailable(),
			RoadTraffic.unavailable(), null, List.of(), List.of(), NearbyMobility.unavailable());
	}
}
