package com.example.musinsaPointSystem.data.apiService;

import org.springframework.stereotype.Service;

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
	private final ApiService apiService;
	private final ObjectMapper objectMapper;
	private final MobilityPerformanceMetrics metrics;

	public MobilityDecision recommend(
		String requestKey,
		MobilityContext context
	) {
		SeoulCityData cityData = apiService.getCityData(context.areaCode());
		MobilityDecision decision = cacheService.find(requestKey)
			.orElseGet(() -> {
				try {
					String json = mobilityAgentService.recommend(context, cityData.toPromptSummary());
					MobilityDecision generatedDecision =
						metrics.record(
							"structured-output",
							() -> readDecision(json)
						);

					generatedDecision = generatedDecision.withoutWeather();
					cacheService.save(requestKey, generatedDecision);
					return generatedDecision;
				} catch (Exception e) {
					throw new RuntimeException("Mobility decision generation failed", e);
				}
			});
		return decision.withWeather(cityData.weather());
	}

	private MobilityDecision readDecision(String json) {
		try {
			return objectMapper.readValue(json, MobilityDecision.class);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid AI structured output", e);
		}
	}
}
