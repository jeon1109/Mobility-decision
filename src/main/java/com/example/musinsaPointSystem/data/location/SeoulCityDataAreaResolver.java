package com.example.musinsaPointSystem.data.location;

import java.math.BigDecimal;
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
	private static final List<AreaPoint> AREAS = List.of(
		point("POI009", "광화문·덕수궁", "37.5759", "126.9768"),
		point("POI014", "강남역", "37.4979", "127.0276"),
		point("POI033", "서울역", "37.5547", "126.9707"),
		point("POI055", "홍대입구역(2호선)", "37.5572", "126.9254"),
		point("POI119", "잠실역", "37.5133", "127.1001"),
		point("POI017", "고속터미널역", "37.5048", "127.0049"),
		point("POI003", "명동 관광특구", "37.5636", "126.9860"),
		point("POI004", "이태원 관광특구", "37.5345", "126.9946"),
		point("POI072", "여의도", "37.5219", "126.9245"),
		point("POI015", "건대입구역", "37.5404", "127.0692")
	);

	@Override
	public Optional<SeoulArea> resolve(Location location) {
		if (location == null || location.latitude() == null || location.longitude() == null
			|| !StringUtils.hasText(location.city()) || !location.city().startsWith("서울")) {
			return Optional.empty();
		}
		AreaPoint nearest = AREAS.stream().min((left, right) -> Double.compare(
			distance(location, left), distance(location, right))).orElse(null);
		if (nearest == null || distance(location, nearest) > MAX_DISTANCE_KM) return Optional.empty();
		return Optional.of(nearest.area());
	}

	private double distance(Location location, AreaPoint point) {
		double lat1 = Math.toRadians(location.latitude().doubleValue());
		double lat2 = Math.toRadians(point.latitude().doubleValue());
		double dLat = lat2 - lat1;
		double dLon = Math.toRadians(point.longitude().doubleValue() - location.longitude().doubleValue());
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}

	private static AreaPoint point(String code, String name, String latitude, String longitude) {
		return new AreaPoint(new SeoulArea(code, name), new BigDecimal(latitude), new BigDecimal(longitude));
	}

	private record AreaPoint(SeoulArea area, BigDecimal latitude, BigDecimal longitude) {}
}
