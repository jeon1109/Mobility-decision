package com.example.musinsaPointSystem.data.apiService;

import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.common.error.AiCommunicationException;
import com.example.musinsaPointSystem.data.traffic.TrafficDataProvider;
import com.example.musinsaPointSystem.dto.MobilityContext;
import com.example.musinsaPointSystem.dto.MobilityDecision;
import com.example.musinsaPointSystem.dto.SeoulCityData;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationService {
	private final RecommendationCacheService cacheService;
	private final MobilityAgentService mobilityAgentService;
	private final TrafficDataProvider trafficDataProvider;
	private final ObjectMapper objectMapper;
	private final MobilityPerformanceMetrics metrics;

	public MobilityDecision recommend(String requestKey, MobilityContext context) {
		SeoulCityData cityData = trafficDataProvider.getCityData(context.areaCode(), context.areaName());
		MobilityDecision decision = cacheService.find(requestKey).orElseGet(() -> {
			String json = mobilityAgentService.recommend(context, cityData.toPromptSummary());
			MobilityDecision generated = metrics.record("structured-output", () -> readDecision(json));
			generated = generated.withoutWeather();
			cacheService.save(requestKey, generated);
			return generated;
		});
		return decision.withWeather(cityData.weather());
	}

	private MobilityDecision readDecision(String json) {
		try {
			return objectMapper.readValue(json, MobilityDecision.class);
		} catch (Exception e) {
			throw new AiCommunicationException("AI 응답 형식이 올바르지 않습니다.", e);
		}
	}
}
