package com.example.musinsaPointSystem.config;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.musinsaPointSystem.data.traffic.TrafficDataProvider;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SeoulCityDataTool {
	private final TrafficDataProvider trafficDataProvider;
	private final MobilityPerformanceMetrics metrics;

	@Tool(description = "서울 주요 장소의 실시간 혼잡도, 도로소통, 대중교통, 날씨 정보를 조회합니다.")
	public String getSeoulCityData(String areaCode) {
		try {
			String result = metrics.record(
				"spring-ai.tool-calling",
				() -> trafficDataProvider.getCityData(areaCode).toPromptSummary()
			);
			metrics.recordToolCall("seoul-city-data", "success");
			return result;
		} catch (RuntimeException e) {
			metrics.recordToolCall("seoul-city-data", "error");
			throw e;
		}
	}
}
