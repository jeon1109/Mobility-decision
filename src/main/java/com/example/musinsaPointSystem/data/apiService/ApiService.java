package com.example.musinsaPointSystem.data.apiService;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriUtils;

import com.example.musinsaPointSystem.dto.SeoulCityData;
import com.example.musinsaPointSystem.dto.WeatherSnapshot;
import com.example.musinsaPointSystem.dto.mobilityv2.CitySituation;
import com.example.musinsaPointSystem.dto.mobilityv2.CitySituation.AirQuality;
import com.example.musinsaPointSystem.dto.mobilityv2.CitySituation.Congestion;
import com.example.musinsaPointSystem.dto.mobilityv2.CitySituation.CongestionForecast;
import com.example.musinsaPointSystem.dto.mobilityv2.CitySituation.DataStatus;
import com.example.musinsaPointSystem.dto.mobilityv2.CitySituation.NearbyMobility;
import com.example.musinsaPointSystem.dto.mobilityv2.CitySituation.NearbySnapshot;
import com.example.musinsaPointSystem.dto.mobilityv2.CitySituation.RoadTraffic;
import com.example.musinsaPointSystem.dto.mobilityv2.CitySituation.Weather;
import com.example.musinsaPointSystem.dto.mobilityv2.CitySituation.WeatherForecast;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ApiService {
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter PROVIDER_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final DateTimeFormatter FORECAST_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
	private static final String SOURCE = CitySituation.SOURCE;

	private final WebClient webClient;
	private final ObjectMapper objectMapper;
	private final MobilityPerformanceMetrics metrics;
	private final Clock clock;

	@Value("${public.api.key}")
	private String serviceKey;

	public ApiService(WebClient webClient, ObjectMapper objectMapper,
		MobilityPerformanceMetrics metrics, Clock clock) {
		this.webClient = webClient;
		this.objectMapper = objectMapper;
		this.metrics = metrics;
		this.clock = clock;
	}

	public String getCityDataSummary(String areaCode) {
		return getCityData(areaCode).toPromptSummary();
	}

	public SeoulCityData getCityData(String areaCode) {
		ParsedCity parsed = load(areaCode, areaCode);
		return parsed == null ? SeoulCityData.unavailable() : parsed.legacy();
	}

	public CitySituation getCitySituation(String areaCode, String areaName) {
		ParsedCity parsed = load(areaCode, areaName);
		return parsed == null ? CitySituation.unavailable(areaCode, areaName) : parsed.situation();
	}

	private ParsedCity load(String areaCode, String areaName) {
		long startedAt = System.nanoTime();
		try {
			String encodedArea = UriUtils.encodePathSegment(areaCode, StandardCharsets.UTF_8);
			String url = "http://openapi.seoul.go.kr:8088/" + serviceKey
				+ "/json/citydata/1/5/" + encodedArea;
			String raw = metrics.record("public.seoul-city-data", () -> webClient.get().uri(url)
				.retrieve().bodyToMono(String.class).block());
			ParsedCity parsed = parse(raw, areaCode, areaName);
			metrics.recordPublicApiRequest("success", false);
			metrics.recordDuration("public.total", "success", System.nanoTime() - startedAt);
			log.info("서울시 도시데이터 파싱 완료, areaCode={}, congestionStatus={}, weatherStatus={}, "
					+ "roadTrafficStatus={}, weatherForecastCount={}, congestionForecastCount={}",
				areaCode, parsed.situation().congestion().status(), parsed.situation().weather().status(),
				parsed.situation().roadTraffic().status(), parsed.situation().weatherForecasts().size(),
				parsed.situation().congestionForecasts().size());
			return parsed;
		} catch (ProviderResponseException e) {
			log.warn("서울시 도시데이터 응답 오류; 영역별 UNAVAILABLE fallback 사용, areaCode={}, providerCode={}",
				areaCode, e.providerCode());
			metrics.recordPublicApiRequest("provider_error", true);
			metrics.recordDuration("public.total", "fallback", System.nanoTime() - startedAt);
			return null;
		} catch (WebClientResponseException e) {
			log.warn("서울시 도시데이터 HTTP 오류; 영역별 UNAVAILABLE fallback 사용, areaCode={}, httpStatus={}",
				areaCode, e.getStatusCode().value());
			metrics.recordPublicApiRequest("http_error", true);
			metrics.recordDuration("public.total", "fallback", System.nanoTime() - startedAt);
			return null;
		} catch (RuntimeException e) {
			log.warn("서울시 도시데이터 조회 실패; 영역별 UNAVAILABLE fallback 사용, areaCode={}, errorType={}",
				areaCode, e.getClass().getSimpleName());
			metrics.recordPublicApiRequest("error", true);
			metrics.recordDuration("public.total", "fallback", System.nanoTime() - startedAt);
			return null;
		}
	}

	private ParsedCity parse(String raw, String requestedCode, String requestedName) {
		try {
			JsonNode root = objectMapper.readTree(raw);
			validateProviderResult(root);
			JsonNode city = locateCityData(root);
			String areaCode = valueOr(text(city, "AREA_CD"), requestedCode);
			String areaName = valueOr(text(city, "AREA_NM"), requestedName);

			JsonNode population = first(city.path("LIVE_PPLTN_STTS"), "LIVE_PPLTN_STTS");
			String congestionLevel = text(population, "AREA_CONGEST_LVL");
			String populationTime = text(population, "PPLTN_TIME");
			Congestion congestion = congestionLevel == null
				? Congestion.unavailable()
				: new Congestion(status(populationTime, Duration.ofMinutes(30)), congestionLevel,
					text(population, "AREA_CONGEST_MSG"), integer(population, "AREA_PPLTN_MIN"),
					integer(population, "AREA_PPLTN_MAX"), normalizeTime(populationTime), SOURCE);

			JsonNode weatherNode = first(city.path("WEATHER_STTS"), "WEATHER_STTS");
			Double temperature = number(weatherNode, "TEMP");
			String weatherTime = text(weatherNode, "WEATHER_TIME");
			DataStatus weatherStatus = temperature == null ? DataStatus.UNAVAILABLE
				: status(weatherTime, Duration.ofMinutes(90));
			Weather weather = weatherStatus == DataStatus.UNAVAILABLE
				? Weather.unavailable()
				: new Weather(weatherStatus, temperature, text(weatherNode, "SKY_STTS"),
					text(weatherNode, "PRECPT_TYPE"), number(weatherNode, "HUMIDITY"),
					normalizeTime(weatherTime), SOURCE);

			JsonNode roadNode = city.path("ROAD_TRAFFIC_STTS");
			if (roadNode.isArray()) roadNode = roadNode.path(0);
			JsonNode averageRoad = first(roadNode.path("AVG_ROAD_DATA"), "AVG_ROAD_DATA");
			String roadLevel = text(averageRoad, "ROAD_TRAFFIC_IDX");
			String roadTime = text(averageRoad, "ROAD_TRAFFIC_TIME");
			RoadTraffic road = roadLevel == null
				? RoadTraffic.unavailable()
				: new RoadTraffic(status(roadTime, Duration.ofMinutes(30)), roadLevel,
					number(averageRoad, "ROAD_TRAFFIC_SPD"), text(averageRoad, "ROAD_MSG"),
					normalizeTime(roadTime), SOURCE);

			AirQuality air = parseAirQuality(weatherNode);
			List<WeatherForecast> weatherForecasts = parseWeatherForecasts(weatherNode);
			List<CongestionForecast> congestionForecasts = parseCongestionForecasts(population);
			CitySituation situation = new CitySituation(areaCode, areaName, normalize(congestion),
				normalize(weather), normalize(road), air, weatherForecasts, congestionForecasts,
				parseNearbyMobility(city));
			WeatherSnapshot legacyWeather = temperature == null
				? WeatherSnapshot.unavailable()
				: new WeatherSnapshot(temperature, text(weatherNode, "SKY_STTS"), weatherTime,
					SOURCE, true, weatherStatus != DataStatus.LIVE);
			return new ParsedCity(new SeoulCityData(valueOr(congestionLevel, "정보 없음"), legacyWeather), situation);
		} catch (ProviderResponseException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid Seoul city data response", e);
		}
	}

	private AirQuality parseAirQuality(JsonNode weather) {
		Double pm10 = number(weather, "PM10");
		Double pm25 = number(weather, "PM25");
		if (pm10 == null && pm25 == null) return null;
		String observedAt = text(weather, "WEATHER_TIME");
		DataStatus status = status(observedAt, Duration.ofMinutes(90));
		if (status == DataStatus.UNAVAILABLE) return null;
		return new AirQuality(status, pm10, text(weather, "PM10_INDEX"), pm25,
			text(weather, "PM25_INDEX"), normalizeTime(observedAt), SOURCE);
	}

	private List<WeatherForecast> parseWeatherForecasts(JsonNode weather) {
		JsonNode forecasts = unwrap(weather.path("FCST24HOURS"), "FCST24HOURS");
		List<WeatherForecast> result = new ArrayList<>();
		for (JsonNode forecast : iterable(forecasts)) {
			String forecastAt = normalizeTime(text(forecast, "FCST_DT"));
			if (forecastAt == null) continue;
			result.add(new WeatherForecast(forecastAt,
				text(forecast, "SKY_STTS"), text(forecast, "PRECPT_TYPE"),
				number(forecast, "RAIN_CHANCE"), number(forecast, "TEMP"), SOURCE));
		}
		return List.copyOf(result);
	}

	private List<CongestionForecast> parseCongestionForecasts(JsonNode population) {
		JsonNode forecasts = unwrap(population.path("FCST_PPLTN"), "FCST_PPLTN");
		List<CongestionForecast> result = new ArrayList<>();
		for (JsonNode forecast : iterable(forecasts)) {
			String forecastAt = normalizeTime(text(forecast, "FCST_TIME"));
			if (forecastAt == null) continue;
			result.add(new CongestionForecast(forecastAt,
				text(forecast, "FCST_CONGEST_LVL"), integer(forecast, "FCST_PPLTN_MIN"),
				integer(forecast, "FCST_PPLTN_MAX"), SOURCE));
		}
		return List.copyOf(result);
	}

	private NearbyMobility parseNearbyMobility(JsonNode city) {
		return new NearbyMobility(
			nearbySnapshot(city, "SUB_STTS"),
			nearbySnapshot(city, "BUS_STN_STTS"),
			nearbySnapshot(city, "SBIKE_STTS"),
			nearbySnapshot(city, "PRK_STTS"),
			nearbySnapshot(city, "CHARGER_STTS")
		);
	}

	private NearbySnapshot nearbySnapshot(JsonNode city, String field) {
		JsonNode wrapper = city.path(field);
		if (wrapper.isMissingNode() || wrapper.isNull()) return NearbySnapshot.unavailable();
		JsonNode values = unwrap(wrapper, field);
		if (values.isObject() && values.isEmpty()) {
			return new NearbySnapshot(DataStatus.LIVE, 0, null, SOURCE);
		}
		int count = 0;
		for (JsonNode ignored : iterable(values)) count++;
		return new NearbySnapshot(DataStatus.LIVE, count, null, SOURCE);
	}

	private DataStatus status(String providerTime, Duration maxAge) {
		if (providerTime == null) return DataStatus.STALE;
		try {
			ZonedDateTime observed = LocalDateTime.parse(providerTime, PROVIDER_TIME).atZone(SEOUL);
			ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(SEOUL);
			if (observed.isAfter(now.plusMinutes(10))) return DataStatus.UNAVAILABLE;
			return observed.isBefore(now.minus(maxAge)) ? DataStatus.STALE : DataStatus.LIVE;
		} catch (RuntimeException e) {
			return DataStatus.STALE;
		}
	}

	private String normalizeTime(String value) {
		if (value == null) return null;
		try {
			return LocalDateTime.parse(value, PROVIDER_TIME).atZone(SEOUL).toOffsetDateTime().toString();
		} catch (RuntimeException e) {
			try {
				return LocalDateTime.parse(value, FORECAST_TIME).atZone(SEOUL).toOffsetDateTime().toString();
			} catch (RuntimeException ignored) {
				return null;
			}
		}
	}

	private Congestion normalize(Congestion value) {
		return value.status() == DataStatus.UNAVAILABLE ? Congestion.unavailable() : value;
	}
	private Weather normalize(Weather value) {
		return value.status() == DataStatus.UNAVAILABLE ? Weather.unavailable() : value;
	}
	private RoadTraffic normalize(RoadTraffic value) {
		return value.status() == DataStatus.UNAVAILABLE ? RoadTraffic.unavailable() : value;
	}

	private JsonNode locateCityData(JsonNode root) {
		JsonNode payload = providerPayload(root);
		JsonNode city = first(payload.path("CITYDATA"), "CITYDATA");
		if (city.isMissingNode() || city.isNull() || !city.isObject() || city.isEmpty()) {
			throw new ProviderResponseException("CITYDATA_MISSING");
		}
		return city;
	}
	private void validateProviderResult(JsonNode root) {
		JsonNode result = providerPayload(root).path("RESULT");
		String code = text(result, "RESULT.CODE");
		if (code != null && !"INFO-000".equals(code)) {
			throw new ProviderResponseException(code);
		}
	}
	private JsonNode providerPayload(JsonNode root) {
		JsonNode wrapped = root.path("SeoulRtd.citydata");
		return wrapped.isMissingNode() || wrapped.isNull() ? root : wrapped;
	}
	private JsonNode first(JsonNode node, String wrapper) {
		JsonNode value = unwrap(node, wrapper);
		return value.isArray() ? value.path(0) : value;
	}
	private JsonNode unwrap(JsonNode node, String wrapper) {
		return node.isObject() && node.has(wrapper) ? node.path(wrapper) : node;
	}
	private Iterable<JsonNode> iterable(JsonNode node) {
		return node.isArray() ? node : node.isMissingNode() || node.isNull() ? List.of() : List.of(node);
	}
	private String text(JsonNode node, String field) {
		String value = node.path(field).asText(null);
		return value == null || value.isBlank() ? null : value.trim();
	}
	private Double number(JsonNode node, String field) {
		String value = text(node, field);
		try { return value == null ? null : Double.valueOf(value); }
		catch (NumberFormatException e) { return null; }
	}
	private Integer integer(JsonNode node, String field) {
		Double value = number(node, field);
		return value == null ? null : value.intValue();
	}
	private String valueOr(String value, String fallback) {
		return value == null ? fallback : value;
	}

	private record ParsedCity(SeoulCityData legacy, CitySituation situation) {}

	private static final class ProviderResponseException extends RuntimeException {
		private final String providerCode;
		private ProviderResponseException(String providerCode) {
			super("Seoul CityData provider error: " + providerCode);
			this.providerCode = providerCode;
		}
		private String providerCode() { return providerCode; }
	}
}
