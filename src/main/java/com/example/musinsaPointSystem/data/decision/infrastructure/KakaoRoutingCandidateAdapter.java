package com.example.musinsaPointSystem.data.decision.infrastructure;

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

import com.example.musinsaPointSystem.data.decision.model.MobilityCandidate;
import com.example.musinsaPointSystem.data.decision.model.MobilityCandidate.Reliability;
import com.example.musinsaPointSystem.data.decision.model.MobilityCandidate.TransportType;
import com.example.musinsaPointSystem.data.decision.port.MobilityCandidateProvider;
import com.example.musinsaPointSystem.data.location.LocationProperties;
import com.example.musinsaPointSystem.data.location.model.Location;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class KakaoRoutingCandidateAdapter implements MobilityCandidateProvider {
	private static final Logger log = LoggerFactory.getLogger(KakaoRoutingCandidateAdapter.class);
	private static final Duration TIMEOUT = Duration.ofSeconds(5);
	private final WebClient webClient;
	private final LocationProperties properties;
	private final MobilityPerformanceMetrics metrics;
	private final ExternalCallExecutor externalCalls;

	public KakaoRoutingCandidateAdapter(WebClient webClient, LocationProperties properties,
		MobilityPerformanceMetrics metrics, ExternalCallExecutor externalCalls) {
		this.webClient = webClient;
		this.properties = properties;
		this.metrics = metrics;
		this.externalCalls = externalCalls;
	}

	@Override
	public List<MobilityCandidate> findCandidates(Location origin, Location destination) {
		if (!valid(origin) || !valid(destination) || !StringUtils.hasText(properties.getApiKey())) return List.of();
		List<MobilityCandidate> result = new ArrayList<>();
		try { result.addAll(transit(origin, destination)); }
		catch (RuntimeException e) { log.warn("Transit candidate lookup failed, errorType={}", e.getClass().getSimpleName()); }
		try {
			MobilityCandidate walking = walking(origin, destination);
			if (walking != null) result.add(walking);
		} catch (RuntimeException e) { log.warn("Walking candidate lookup failed, errorType={}", e.getClass().getSimpleName()); }
		return List.copyOf(result);
	}

	private List<MobilityCandidate> transit(Location origin, Location destination) {
		JsonNode body = get("/v2/routing/publictraffic", origin, destination, "candidate.collection.transit");
		if (!"OK".equals(body.path("status").asText())) return List.of();
		List<MobilityCandidate> values = new ArrayList<>();
		int index = 0;
		for (JsonNode route : body.path("routes")) {
			JsonNode properties = route.path("properties");
			Integer total = integer(properties, "totalTime");
			if (total == null) continue;
			Integer walking = 0;
			List<String> summaries = new ArrayList<>();
			for (JsonNode step : route.path("steps")) {
				JsonNode stepProperties = step.path("properties");
				if ("WALKING".equals(stepProperties.path("type").asText()))
					walking += value(integer(stepProperties, "time"));
				String guidance = text(stepProperties, "guidance");
				if (guidance != null && summaries.size() < 4) summaries.add(guidance);
			}
			values.add(new MobilityCandidate("kakao-transit-" + index++, type(text(properties, "type")),
				origin, destination, total, walking, null, integer(properties, "transfers"),
				integer(properties.path("fare"), "value"), Reliability.MEDIUM,
				String.join(" → ", summaries), "KAKAO_ROUTING"));
			if (values.size() >= 5) break;
		}
		return values;
	}

	private MobilityCandidate walking(Location origin, Location destination) {
		JsonNode body = get("/v2/routing/walk", origin, destination, "candidate.collection.walk");
		if (!"OK".equals(body.path("status").asText("OK"))) return null;
		JsonNode route = body.path("route");
		JsonNode routeProperties = route.path("properties");
		Integer total = integer(routeProperties, "totalTime");
		if (total == null) return null;
		return new MobilityCandidate("kakao-walk", TransportType.WALK, origin, destination,
			total, total, 0, 0, 0, Reliability.HIGH, "도보 경로", "KAKAO_ROUTING");
	}

	private JsonNode get(String path, Location origin, Location destination, String stage) {
		URI uri = UriComponentsBuilder.fromUriString(properties.getBaseUrl()).path(path)
			.queryParam("start_x", origin.longitude().toPlainString())
			.queryParam("start_y", origin.latitude().toPlainString())
			.queryParam("end_x", destination.longitude().toPlainString())
			.queryParam("end_y", destination.latitude().toPlainString())
			.queryParam("s_name", origin.name()).queryParam("e_name", destination.name())
			.build().encode().toUri();
		JsonNode body = metrics.record(stage, () -> externalCalls.execute("kakao-routing", () -> webClient.get().uri(uri)
			.header(HttpHeaders.AUTHORIZATION, properties.getAuthorizationHeader())
			.retrieve().bodyToMono(JsonNode.class).timeout(TIMEOUT).block()));
		if (body == null) throw new IllegalStateException("Empty routing response");
		return body;
	}

	private boolean valid(Location value) {
		return value != null && value.latitude() != null && value.longitude() != null;
	}

	private TransportType type(String value) {
		return switch (value == null ? "" : value) {
			case "BUS" -> TransportType.BUS; case "SUBWAY" -> TransportType.SUBWAY;
			default -> TransportType.MIXED_TRANSIT;
		};
	}

	private String text(JsonNode node, String field) {
		String value = node.path(field).asText(null);
		return value == null || value.isBlank() ? null : value.trim();
	}

	private Integer integer(JsonNode node, String field) {
		return node.hasNonNull(field) && node.path(field).canConvertToInt() ? node.path(field).asInt() : null;
	}

	private int value(Integer value) { return value == null ? 0 : value; }
}
