package com.example.musinsaPointSystem.data.decision.infrastructure;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.musinsaPointSystem.data.decision.SubwayNaming;
import com.example.musinsaPointSystem.data.decision.model.SubwayArrivalEvidence;
import com.example.musinsaPointSystem.data.decision.port.SubwayArrivalPort;
import com.example.musinsaPointSystem.data.evidence.FreshnessStatus;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class SeoulSubwayArrivalAdapter implements SubwayArrivalPort {
	private static final Logger log = LoggerFactory.getLogger(SeoulSubwayArrivalAdapter.class);
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter PROVIDER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S");
	private static final Duration TIMEOUT = Duration.ofSeconds(4);
	private final WebClient webClient;
	private final MobilityPerformanceMetrics metrics;
	private final Clock clock;
	private final String apiKey;
	private final String baseUrl;
	private final ExternalCallExecutor externalCalls;

	public SeoulSubwayArrivalAdapter(WebClient webClient, MobilityPerformanceMetrics metrics, Clock clock,
		@Value("${seoul.open-api.key:}") String apiKey,
		@Value("${seoul.subway-arrival.base-url:http://swopenapi.seoul.go.kr/api/subway}") String baseUrl,
		ExternalCallExecutor externalCalls) {
		this.webClient = webClient;
		this.metrics = metrics;
		this.clock = clock;
		this.apiKey = apiKey;
		this.baseUrl = baseUrl;
		this.externalCalls = externalCalls;
	}

	@Override
	public List<SubwayArrivalEvidence> findArrivals(String stationName) {
		if (!StringUtils.hasText(stationName)) return List.of();
		if (!StringUtils.hasText(apiKey)) {
			log.warn("Subway arrival lookup skipped; SEOUL_OPEN_API_KEY is not configured");
			return List.of();
		}
		try {
			String queryStation = SubwayNaming.stationQueryName(stationName);
			URI uri = UriComponentsBuilder.fromUriString(baseUrl)
				.pathSegment(apiKey, "json", "realtimeStationArrival", "0", "20", queryStation)
				.build().encode().toUri();
			JsonNode body = metrics.record("evidence.subway", () -> externalCalls.execute("seoul-subway-arrival",
				() -> webClient.get().uri(uri).retrieve().bodyToMono(JsonNode.class).timeout(TIMEOUT).block()));
			if (body == null) {
				log.warn("Subway arrival lookup returned an empty response, station={}, query={}",
					stationName, queryStation);
				return List.of();
			}
			String resultCode = body.path("errorMessage").path("code").asText("INFO-000");
			if (!"INFO-000".equals(resultCode)) {
				log.warn("Subway arrival provider rejected the request, station={}, query={}, code={}, message={}",
					stationName, queryStation, resultCode,
					body.path("errorMessage").path("message").asText("unknown"));
				return List.of();
			}
			String receivedAt = java.time.ZonedDateTime.now(clock).withZoneSameInstant(SEOUL)
				.toOffsetDateTime().toString();
			List<SubwayArrivalEvidence> result = new ArrayList<>();
			for (JsonNode node : body.path("realtimeArrivalList")) {
				Integer seconds = integer(node, "barvlDt");
				String observedAt = normalize(text(node, "recptnDt"));
				result.add(new SubwayArrivalEvidence(text(node, "statnNm"), line(text(node, "subwayId")),
					text(node, "updnLine"), seconds, text(node, "arvlMsg2"), receivedAt, observedAt,
					"SEOUL_SUBWAY_ARRIVAL", freshness(observedAt)));
			}
			if (result.isEmpty()) {
				log.info("Subway arrival provider returned no trains, station={}, query={}",
					stationName, queryStation);
			}
			return List.copyOf(result);
		} catch (RuntimeException e) {
			log.warn("Subway arrival lookup failed; returning unavailable evidence, station={}, errorType={}",
				stationName, e.getClass().getSimpleName());
			return List.of();
		}
	}

	private FreshnessStatus freshness(String observedAt) {
		if (observedAt == null) return FreshnessStatus.UNKNOWN;
		try {
			var observed = java.time.OffsetDateTime.parse(observedAt).toInstant();
			return observed.isBefore(clock.instant().minus(Duration.ofMinutes(3)))
				? FreshnessStatus.STALE : FreshnessStatus.FRESH;
		} catch (RuntimeException e) { return FreshnessStatus.UNKNOWN; }
	}

	private String normalize(String value) {
		if (value == null) return null;
		try { return LocalDateTime.parse(value, PROVIDER_TIME).atZone(SEOUL).toOffsetDateTime().toString(); }
		catch (RuntimeException e) {
			try { return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
				.atZone(SEOUL).toOffsetDateTime().toString(); }
			catch (RuntimeException ignored) { return null; }
		}
	}

	private String line(String subwayId) {
		return switch (subwayId == null ? "" : subwayId) {
			case "1001" -> "1호선"; case "1002" -> "2호선"; case "1003" -> "3호선";
			case "1004" -> "4호선"; case "1005" -> "5호선"; case "1006" -> "6호선";
			case "1007" -> "7호선"; case "1008" -> "8호선"; case "1009" -> "9호선";
			case "1063" -> "경의중앙선"; case "1065" -> "공항철도"; case "1067" -> "경춘선";
			case "1075" -> "수인분당선"; case "1077" -> "신분당선"; case "1092" -> "우이신설선";
			case "1032" -> "GTX-A"; default -> subwayId;
		};
	}

	private String text(JsonNode node, String field) {
		String value = node.path(field).asText(null);
		return value == null || value.isBlank() ? null : value.trim();
	}

	private Integer integer(JsonNode node, String field) {
		try { return Integer.valueOf(text(node, field)); } catch (RuntimeException e) { return null; }
	}
}
