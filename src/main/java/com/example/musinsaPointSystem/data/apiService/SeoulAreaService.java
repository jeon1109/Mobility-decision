package com.example.musinsaPointSystem.data.apiService;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.dto.SeoulArea;

@Service
public class SeoulAreaService {
	public List<SeoulArea> getAreas() {
		return List.of(
			new SeoulArea("POI009", "광화문·덕수궁"),
			new SeoulArea("POI014", "강남역"),
			new SeoulArea("POI039", "서울역"),
			new SeoulArea("POI045", "홍대입구역"),
			new SeoulArea("POI046", "잠실역"),
			new SeoulArea("POI021", "고속터미널역"),
			new SeoulArea("POI033", "명동 관광특구"),
			new SeoulArea("POI035", "이태원 관광특구"),
			new SeoulArea("POI028", "여의도"),
			new SeoulArea("POI019", "건대입구역")
		);
	}
}
