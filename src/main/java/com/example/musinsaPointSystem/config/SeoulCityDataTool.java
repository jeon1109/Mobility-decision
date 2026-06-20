package com.example.musinsaPointSystem.config;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.musinsaPointSystem.data.apiService.ApiService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SeoulCityDataTool {
	private final ApiService seoulCityDataService;

	@Tool(description = "서울 주요 장소의 실시간 혼잡도, 도로소통, 대중교통, 날씨 정보를 조회합니다.")
	public String getSeoulCityData(String areaCode) {
		return seoulCityDataService.getCityDataSummary(areaCode);
	}
}
