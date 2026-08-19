package com.example.musinsaPointSystem.data.apiService;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.musinsaPointSystem.dto.SeoulCityData;
import com.example.musinsaPointSystem.dto.WeatherSnapshot;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiService {
	private final WebClient webClient;

	@Value("${public.api.key}")
	private String serviceKey;

	private final ObjectMapper objectMapper;
	private final MobilityPerformanceMetrics metrics;

	public String getCityDataSummary(String areaCode) {
		return getCityData(areaCode).toPromptSummary();
	}

	public SeoulCityData getCityData(String areaCode) {
		long startedAt = System.nanoTime();
		try {
			String encodedArea =
				UriUtils.encodePathSegment(
					areaCode,
					StandardCharsets.UTF_8
				);

			String url =
				"http://openapi.seoul.go.kr:8088/"
					+ serviceKey
					+ "/json/citydata/1/5/"
					+ encodedArea;

			String raw = metrics.record(
				"public.seoul-city-data",
				() -> webClient.get()
					.uri(url)
					.retrieve()
					.bodyToMono(String.class)
					.block()
			);

			SeoulCityData cityData = parse(raw);
			metrics.recordPublicApiRequest("success", false);
			metrics.recordDuration("public.total", "success", System.nanoTime() - startedAt);
			return cityData;
		} catch (Exception e) {
			log.warn(
				"서울시 도시데이터 조회 실패; fallback 사용, areaCode={}, errorType={}",
				areaCode,
				e.getClass().getSimpleName()
			);
			metrics.recordPublicApiRequest("error", true);
			metrics.recordDuration("public.total", "fallback", System.nanoTime() - startedAt);
			return SeoulCityData.unavailable();
		}
	}

	private SeoulCityData parse(String raw) {
		try {
			JsonNode root = objectMapper.readTree(raw);
			JsonNode cityData = locateCityData(root);
			JsonNode population = firstEntry(cityData.path("LIVE_PPLTN_STTS"), "LIVE_PPLTN_STTS");
			JsonNode weather = firstEntry(cityData.path("WEATHER_STTS"), "WEATHER_STTS");

			String congestion = textOrNull(population, "AREA_CONGEST_LVL");
			Double temperature = numberOrNull(weather, "TEMP");
			String condition = textOrNull(weather, "SKY_STTS");
			if (condition == null) {
				JsonNode forecast = firstEntry(weather.path("FCST24HOURS"), "FCST24HOURS");
				condition = textOrNull(forecast, "SKY_STTS");
			}

			if (temperature == null) {
				throw new IllegalArgumentException("Seoul city data response has no current temperature");
			}

			WeatherSnapshot snapshot = new WeatherSnapshot(
				temperature,
				condition,
				textOrNull(weather, "WEATHER_TIME"),
				"SEOUL_CITY_DATA",
				true,
				false
			);
			return new SeoulCityData(congestion == null ? "정보 없음" : congestion, snapshot);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid Seoul city data response", e);
		}
	}

	private JsonNode locateCityData(JsonNode root) {
		JsonNode cityData = root.path("CITYDATA");
		if (cityData.isMissingNode()) {
			cityData = root.path("SeoulRtd.citydata").path("CITYDATA");
		}
		return firstEntry(cityData, "CITYDATA");
	}

	private JsonNode firstEntry(JsonNode node, String wrapperName) {
		JsonNode current = node;
		if (current.isObject() && current.has(wrapperName)) {
			current = current.path(wrapperName);
		}
		return current.isArray() ? current.path(0) : current;
	}

	private String textOrNull(JsonNode node, String field) {
		String value = node.path(field).asText(null);
		return value == null || value.isBlank() ? null : value.trim();
	}

	private Double numberOrNull(JsonNode node, String field) {
		String value = textOrNull(node, field);
		if (value == null) {
			return null;
		}
		try {
			return Double.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
