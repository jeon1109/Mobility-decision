package com.example.musinsaPointSystem.data.location;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.musinsaPointSystem.data.location.model.AdministrativeArea;
import com.example.musinsaPointSystem.data.location.model.Location;
import com.example.musinsaPointSystem.data.location.model.PlaceCandidate;
import com.example.musinsaPointSystem.data.location.model.ResolvedLocation;
import com.example.musinsaPointSystem.data.location.port.CityDataAreaResolver;
import com.example.musinsaPointSystem.data.location.port.PlaceSearchPort;
import com.example.musinsaPointSystem.data.location.port.ReverseGeocodingPort;
import com.example.musinsaPointSystem.dto.SeoulArea;

@Service
public class PlaceSearchUseCase {
	private final PlaceSearchPort placeSearchPort;
	private final ReverseGeocodingPort reverseGeocodingPort;
	private final CityDataAreaResolver cityDataAreaResolver;

	public PlaceSearchUseCase(PlaceSearchPort placeSearchPort, ReverseGeocodingPort reverseGeocodingPort,
		CityDataAreaResolver cityDataAreaResolver) {
		this.placeSearchPort = placeSearchPort;
		this.reverseGeocodingPort = reverseGeocodingPort;
		this.cityDataAreaResolver = cityDataAreaResolver;
	}

	public List<PlaceCandidate> search(String keyword) {
		String normalized = keyword == null ? "" : keyword.trim().replaceAll("\\s+", " ");
		if (normalized.length() < 2) throw new IllegalArgumentException("검색어는 두 글자 이상 입력해주세요.");
		if (normalized.length() > 80) throw new IllegalArgumentException("검색어는 80자 이하로 입력해주세요.");
		return placeSearchPort.search(normalized);
	}

	public ResolvedLocation resolve(PlaceCandidate candidate) {
		validate(candidate);
		AdministrativeArea administrativeArea = reverseGeocodingPort.resolve(
			candidate.latitude(), candidate.longitude());
		Location location = new Location(candidate.providerPlaceId(), candidate.name(), candidate.address(),
			candidate.roadAddress(), candidate.latitude(), candidate.longitude(),
			administrativeArea.city(), administrativeArea.district(), candidate.category(), candidate.provider());
		return resolved(location);
	}

	public ResolvedLocation resolveCoordinates(BigDecimal latitude, BigDecimal longitude) {
		validateCoordinates(latitude, longitude);
		AdministrativeArea administrativeArea = reverseGeocodingPort.resolve(latitude, longitude);
		Location location = new Location("current-location", "현재 위치", null, null, latitude, longitude,
			administrativeArea.city(), administrativeArea.district(), null, "DEVICE_GEOLOCATION");
		return resolved(location);
	}

	private ResolvedLocation resolved(Location location) {
		SeoulArea area = cityDataAreaResolver.resolve(location).orElse(null);
		if (area != null) return new ResolvedLocation(location, area, true, null);
		String message = StringUtils.hasText(location.city()) && location.city().startsWith("서울")
			? "선택한 장소는 현재 서울 실시간 도시정보 지원 지역과 연결되지 않습니다."
			: "현재 실시간 도시정보는 서울 지역을 우선 지원합니다.";
		return new ResolvedLocation(location, null, false, message);
	}

	private void validate(PlaceCandidate value) {
		if (value == null || !StringUtils.hasText(value.providerPlaceId()) || !StringUtils.hasText(value.name())) {
			throw new IllegalArgumentException("선택한 장소 정보가 올바르지 않습니다.");
		}
		validateCoordinates(value.latitude(), value.longitude());
	}

	private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
		if (latitude == null || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
			|| latitude.compareTo(BigDecimal.valueOf(90)) > 0
			|| longitude == null || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
			|| longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
			throw new IllegalArgumentException("장소 좌표가 올바르지 않습니다.");
		}
	}
}
