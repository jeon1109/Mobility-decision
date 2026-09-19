package com.example.musinsaPointSystem.data.evidence;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mobility.freshness")
public class EvidenceFreshnessProperties {
	private Duration weather = Duration.ofMinutes(90);
	private Duration congestion = Duration.ofMinutes(30);
	private Duration traffic = Duration.ofMinutes(30);
	private Duration subway = Duration.ofMinutes(5);
	private Duration futureTolerance = Duration.ofMinutes(10);

	public Duration allowedAge(EvidenceType type) {
		return switch (type) {
			case WEATHER -> weather;
			case CONGESTION -> congestion;
			case TRAFFIC -> traffic;
			case SUBWAY -> subway;
		};
	}

	public Duration getWeather() { return weather; }
	public void setWeather(Duration weather) { this.weather = requirePositive(weather, "weather"); }
	public Duration getCongestion() { return congestion; }
	public void setCongestion(Duration congestion) { this.congestion = requirePositive(congestion, "congestion"); }
	public Duration getTraffic() { return traffic; }
	public void setTraffic(Duration traffic) { this.traffic = requirePositive(traffic, "traffic"); }
	public Duration getSubway() { return subway; }
	public void setSubway(Duration subway) { this.subway = requirePositive(subway, "subway"); }
	public Duration getFutureTolerance() { return futureTolerance; }
	public void setFutureTolerance(Duration futureTolerance) {
		this.futureTolerance = requireNonNegative(futureTolerance, "futureTolerance");
	}

	private Duration requirePositive(Duration value, String name) {
		if (value == null || value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException("mobility.freshness." + name + " must be positive");
		}
		return value;
	}

	private Duration requireNonNegative(Duration value, String name) {
		if (value == null || value.isNegative()) {
			throw new IllegalArgumentException("mobility.freshness." + name + " must not be negative");
		}
		return value;
	}
}
