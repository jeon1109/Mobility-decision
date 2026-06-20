package com.example.musinsaPointSystem.data.apiService;

import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.dto.MobilityContext;
import com.example.musinsaPointSystem.dto.MobilityDecision;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationService {

	private final RecommendationCacheService cacheService;
	private final MobilityAgentService mobilityAgentService;
	private final ObjectMapper objectMapper;

	public MobilityDecision recommend(
		String requestKey,
		MobilityContext context
	) {
		return cacheService.find(requestKey)
			.orElseGet(() -> {
				try {
					String json = mobilityAgentService.recommend(context);
					MobilityDecision decision =
						objectMapper.readValue(
							json,
							MobilityDecision.class
						);

					cacheService.save(requestKey, decision);
					return decision;
				} catch (Exception e) {
					try {
						throw new Exception();
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}
			});
	}
}
