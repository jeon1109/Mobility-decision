package com.example.musinsaPointSystem.data.apiService;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.dto.SeoulArea;

@Service
public class SeoulAreaService {
	private static final List<SeoulArea> AREAS = List.of(
		new SeoulArea("POI009", "광화문·덕수궁"),
		new SeoulArea("POI014", "강남역"),
		new SeoulArea("POI033", "서울역"),
		new SeoulArea("POI055", "홍대입구역(2호선)"),
		new SeoulArea("POI119", "잠실역"),
		new SeoulArea("POI017", "고속터미널역"),
		new SeoulArea("POI003", "명동 관광특구"),
		new SeoulArea("POI004", "이태원 관광특구"),
		new SeoulArea("POI072", "여의도"),
		new SeoulArea("POI015", "건대입구역")
	);

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
