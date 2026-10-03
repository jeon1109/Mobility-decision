package com.example.musinsaPointSystem.data.location;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.musinsaPointSystem.data.location.model.Location;
import com.example.musinsaPointSystem.data.location.port.CityDataAreaResolver;
import com.example.musinsaPointSystem.dto.SeoulArea;

@Component
public class SeoulCityDataAreaResolver implements CityDataAreaResolver {
	private static final double MAX_DISTANCE_KM = 7.0;
	private static final List<SeoulObservationCatalog.Point> AREAS = SeoulObservationCatalog.POINTS;

	@Override
	public Optional<SeoulArea> resolve(Location location) {
		if (location == null || location.latitude() == null || location.longitude() == null
			|| !StringUtils.hasText(location.city()) || !location.city().startsWith("서울")) {
			return Optional.empty();
		}
		SeoulObservationCatalog.Point nearest = AREAS.stream().min((left, right) -> Double.compare(
			distance(location, left), distance(location, right))).orElse(null);
		if (nearest == null || distance(location, nearest) > MAX_DISTANCE_KM)
			return Optional.empty();
		return Optional.of(nearest.area());
	}

	private double distance(Location location, SeoulObservationCatalog.Point point) {
		double lat1 = Math.toRadians(location.latitude().doubleValue());
		double lat2 = Math.toRadians(point.latitude().doubleValue());
		double dLat = lat2 - lat1;
		double dLon = Math.toRadians(point.longitude().doubleValue() - location.longitude().doubleValue());
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

}
