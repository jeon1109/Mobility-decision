package com.example.musinsaPointSystem.data.decision.infrastructure;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.musinsaPointSystem.data.decision.model.NearbyStation;
import com.example.musinsaPointSystem.data.decision.port.NearbyStationPort;
import com.example.musinsaPointSystem.data.location.LocationProperties;
import com.example.musinsaPointSystem.data.location.model.Location;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class KakaoNearbyStationAdapter implements NearbyStationPort {
	private static final Logger log = LoggerFactory.getLogger(KakaoNearbyStationAdapter.class);
	private static final int WALKING_METERS_PER_MINUTE = 75;
	private static final Duration TIMEOUT = Duration.ofSeconds(4);
	private final WebClient webClient;
	private final LocationProperties properties;
	private final MobilityPerformanceMetrics metrics;
	private final ExternalCallExecutor externalCalls;

	public KakaoNearbyStationAdapter(WebClient webClient, LocationProperties properties,
		MobilityPerformanceMetrics metrics, ExternalCallExecutor externalCalls) {
		this.webClient = webClient;
		this.properties = properties;
		this.metrics = metrics;
		this.externalCalls = externalCalls;
	}

	@Override
	public List<NearbyStation> findNearby(Location origin) {
		if (origin == null || origin.latitude() == null || origin.longitude() == null
			|| !StringUtils.hasText(properties.getApiKey())) return List.of();
		try {
			URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl())
				.path("/v2/local/search/category.json")
				.queryParam("category_group_code", "SW8")
				.queryParam("x", origin.longitude().toPlainString())
				.queryParam("y", origin.latitude().toPlainString())
				.queryParam("radius", 3000)
				.queryParam("sort", "distance")
				.queryParam("size", 5).build().encode().toUri();
			JsonNode body = metrics.record("evidence.nearby-station", () -> externalCalls.execute("kakao-nearby-station", () -> webClient.get().uri(uri)
				.header(HttpHeaders.AUTHORIZATION, properties.getAuthorizationHeader())
				.retrieve().bodyToMono(JsonNode.class).timeout(TIMEOUT).block()));
			if (body == null) return List.of();
			List<NearbyStation> result = new ArrayList<>();
			for (JsonNode node : body.path("documents")) {
				Integer distance = integer(node, "distance");
				String name = text(node, "place_name");
				if (!StringUtils.hasText(name)) continue;
				result.add(new NearbyStation(text(node, "id"), name,
					line(text(node, "category_name")), distance, walkingSeconds(distance),
					decimal(node, "y"), decimal(node, "x"), "KAKAO_LOCAL"));
			}
			return List.copyOf(result);
		} catch (RuntimeException e) {
			log.warn("Nearby station lookup failed; returning unavailable evidence, errorType={}",
				e.getClass().getSimpleName());
			return List.of();
		}
	}

	private Integer walkingSeconds(Integer distance) {
		return distance == null ? null : (int) Math.ceil(distance / (double) WALKING_METERS_PER_MINUTE) * 60;
	}

	private String line(String category) {
		if (!StringUtils.hasText(category)) return null;
		String[] values = category.split(">");
		String candidate = values[values.length - 1].trim();
		return candidate.contains("호선") || candidate.contains("선")
			|| candidate.equals("공항철도") || candidate.startsWith("GTX-") ? candidate : null;
	}

	private String text(JsonNode node, String field) {
		String value = node.path(field).asText(null);
		return value == null || value.isBlank() ? null : value.trim();
	}

	private Integer integer(JsonNode node, String field) {
		try { return Integer.valueOf(text(node, field)); } catch (RuntimeException e) { return null; }
	}

	private BigDecimal decimal(JsonNode node, String field) {
		try { return new BigDecimal(text(node, field)); } catch (RuntimeException e) { return null; }
	}
}
