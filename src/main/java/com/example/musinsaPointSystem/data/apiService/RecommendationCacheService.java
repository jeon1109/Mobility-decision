package com.example.musinsaPointSystem.data.apiService;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.dto.MobilityDecision;

@Service
public class RecommendationCacheService {
	private final Map<String, MobilityDecision> cache =
		new ConcurrentHashMap<>();

	public Optional<MobilityDecision> find(String key) {
		return Optional.ofNullable(cache.get(key));
	}

	public void save(String key, MobilityDecision decision) {
		cache.put(key, decision);
	}
}
