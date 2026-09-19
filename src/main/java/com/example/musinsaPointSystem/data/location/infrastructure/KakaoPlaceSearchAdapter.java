package com.example.musinsaPointSystem.data.location.infrastructure;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.musinsaPointSystem.data.location.LocationProperties;
import com.example.musinsaPointSystem.data.location.PlaceProviderException;
import com.example.musinsaPointSystem.data.location.model.AdministrativeArea;
import com.example.musinsaPointSystem.data.location.model.PlaceCandidate;
import com.example.musinsaPointSystem.data.location.port.PlaceSearchPort;
import com.example.musinsaPointSystem.data.location.port.ReverseGeocodingPort;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class KakaoPlaceSearchAdapter implements PlaceSearchPort, ReverseGeocodingPort {
	private static final Duration TIMEOUT = Duration.ofSeconds(4);
	private static final Logger log = LoggerFactory.getLogger(KakaoPlaceSearchAdapter.class);
	private final WebClient webClient;
	private final LocationProperties properties;
	private final MobilityPerformanceMetrics metrics;

	public KakaoPlaceSearchAdapter(WebClient webClient, LocationProperties properties,
		MobilityPerformanceMetrics metrics) {
		this.webClient = webClient;
		this.properties = properties;
		this.metrics = metrics;
	}

	@Override
	public List<PlaceCandidate> search(String keyword) {
		JsonNode body = metrics.record("place.search.duration",
			() -> get("/v2/local/search/keyword.json", keyword, null, null));
		List<PlaceCandidate> values = new ArrayList<>();
		for (JsonNode item : body.path("documents")) {
			BigDecimal latitude = decimal(item, "y");
			BigDecimal longitude = decimal(item, "x");
			String id = text(item, "id");
			String name = text(item, "place_name");
			if (latitude == null || longitude == null || !StringUtils.hasText(id) || !StringUtils.hasText(name))
				continue;
			values.add(new PlaceCandidate(id, name, text(item, "address_name"),
				text(item, "road_address_name"), latitude, longitude,
				text(item, "category_name"), "KAKAO"));
		}
		return List.copyOf(values);
	}

	@Override
	public AdministrativeArea resolve(BigDecimal latitude, BigDecimal longitude) {
		JsonNode body = metrics.record("place.resolve.duration",
			() -> get("/v2/local/geo/coord2regioncode.json", null, longitude, latitude));
		JsonNode selected = null;
		for (JsonNode item : body.path("documents")) {
			if ("H".equals(text(item, "region_type"))) {
				selected = item;
				break;
			}
			if (selected == null)
				selected = item;
		}
		if (selected == null)
			throw new PlaceProviderException("좌표의 행정구역을 확인하지 못했습니다.");
		return new AdministrativeArea(text(selected, "region_1depth_name"),
			text(selected, "region_2depth_name"));
	}

	private JsonNode get(String path, String query, BigDecimal x, BigDecimal y) {
		String apiKey = properties.getApiKey() == null ? "" : properties.getApiKey().trim();
		if (!StringUtils.hasText(apiKey)) {
			throw new PlaceProviderException("장소 검색 API가 설정되지 않았습니다.");
		}
		try {
			UriComponentsBuilder uriBuilder = UriComponentsBuilder
				.fromUriString(properties.getBaseUrl())
				.path(path);
			if (query != null) {
				uriBuilder.queryParam("query", query)
					.queryParam("size", properties.getSearchLimit());
			}
			if (x != null)
				uriBuilder.queryParam("x", x.toPlainString());
			if (y != null)
				uriBuilder.queryParam("y", y.toPlainString());

			return webClient.get()
				.uri(uriBuilder.build().encode().toUri())
				.header(HttpHeaders.AUTHORIZATION, properties.getAuthorizationHeader())
				.retrieve()
				.bodyToMono(JsonNode.class)
				.timeout(TIMEOUT)
				.blockOptional()
				.orElseThrow(() -> new PlaceProviderException("장소 검색 응답이 비어 있습니다."));
		} catch (PlaceProviderException e) {
			throw e;
		} catch (WebClientResponseException e) {
			int status = e.getStatusCode().value();
			log.warn("Kakao place API rejected request: status={}, response={}", status,
				safeProviderMessage(e.getResponseBodyAsString()));
			if (status == 401 || status == 403) {
				throw new PlaceProviderException(
					"Kakao REST API 키 또는 카카오맵 사용 권한을 확인해 주세요. (HTTP " + status + ")", e);
			}
			if (status == 429) {
				throw new PlaceProviderException("Kakao 장소 검색 API 호출 한도를 초과했습니다. (HTTP 429)", e);
			}
			throw new PlaceProviderException("Kakao 장소 검색 API 요청이 거부되었습니다. (HTTP " + status + ")", e);
		} catch (WebClientRequestException e) {
			log.warn("Kakao place API network failure: type={}", e.getClass().getSimpleName(), e);
			throw new PlaceProviderException("Kakao 장소 검색 서버에 연결할 수 없습니다.", e);
		} catch (RuntimeException e) {
			if (hasCause(e, TimeoutException.class)) {
				throw new PlaceProviderException("Kakao 장소 검색 API 응답 시간이 초과되었습니다.", e);
			}
			log.warn("Unexpected Kakao place API failure: type={}", e.getClass().getSimpleName(), e);
			throw new PlaceProviderException("장소 검색 서비스에 연결하지 못했습니다.", e);
		}
	}

	private String safeProviderMessage(String body) {
		if (!StringUtils.hasText(body))
			return "<empty>";
		String normalized = body.replaceAll("[\\r\\n\\t]+", " ").trim();
		return normalized.length() <= 300 ? normalized : normalized.substring(0, 300);
	}

	private boolean hasCause(Throwable error, Class<? extends Throwable> causeType) {
		Throwable current = error;
		while (current != null) {
			if (causeType.isInstance(current))
				return true;
			current = current.getCause();
		}
		return false;
	}

	private String text(JsonNode node, String field) {
		String value = node.path(field).asText(null);
		return value == null || value.isBlank() ? null : value.trim();
	}

	private BigDecimal decimal(JsonNode node, String field) {
		try {
			String value = text(node, field);
			return value == null ? null : new BigDecimal(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
