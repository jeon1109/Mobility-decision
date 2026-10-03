package com.example.musinsaPointSystem.data.apiService;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.dto.SeoulArea;

@Service
public class SeoulAreaService {
	private static final List<SeoulArea> AREAS = com.example.musinsaPointSystem.data.location.SeoulObservationCatalog.POINTS
		.stream().map(com.example.musinsaPointSystem.data.location.SeoulObservationCatalog.Point::area).toList();

	public List<SeoulArea> getAreas() {
		return AREAS;
	}

	public SeoulArea requireArea(String areaCode) {
		return AREAS.stream()
			.filter(area -> area.areaCode().equals(areaCode))
			.findFirst()
			.orElseThrow(() -> new AreaNotFoundException(areaCode));
	}

	public static class AreaNotFoundException extends RuntimeException {
		public AreaNotFoundException(String areaCode) {
			super("지원하지 않는 지역 코드입니다: " + areaCode);
		}
	}
}
