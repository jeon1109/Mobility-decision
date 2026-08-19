package com.example.musinsaPointSystem.data.apiService;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.dto.MobilityDecision;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;

@Service
public class RecommendationCacheService {
	private final Map<String, MobilityDecision> cache =
		new ConcurrentHashMap<>();
	private final MobilityPerformanceMetrics metrics;

	public RecommendationCacheService(MobilityPerformanceMetrics metrics) {
		this.metrics = metrics;
	}

	public Optional<MobilityDecision> find(String key) {
		return metrics.record("cache.lookup", () -> {
			Optional<MobilityDecision> result = Optional.ofNullable(cache.get(key));
			metrics.recordCacheRequest(result.isPresent());
			return result;
		});
	}

	public void save(String key, MobilityDecision decision) {
		metrics.record("cache.save", () -> cache.put(key, decision));
	}
}
