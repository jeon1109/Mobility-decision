package com.example.musinsaPointSystem.dto;

public record WeatherSnapshot(
	Double temperatureCelsius,
	String condition,
	String observedAt,
	String source,
	boolean available,
	boolean stale
) {
	public static WeatherSnapshot unavailable() {
		return new WeatherSnapshot(null, null, null, "SEOUL_CITY_DATA", false, false);
	}
}
