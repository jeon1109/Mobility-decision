package com.example.musinsaPointSystem.data.apiService;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApiService {
	private final WebClient webClient;

	@Value("${public.api.key}")
	private String serviceKey;

	private final ObjectMapper objectMapper;

	public String getCityDataSummary(String areaCode) {

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

			String raw =
				webClient.get()
					.uri(url)
					.retrieve()
					.bodyToMono(String.class)
					.block();

			return summarize(raw);
		} catch (Exception e) {
			e.printStackTrace();
			return """
				도시 데이터를 가져오지 못했습니다.
				""";
		}
	}

	private String summarize(String raw) {
		try {
			JsonNode root = objectMapper.readTree(raw);
			JsonNode cityData = root.path("CITYDATA");

			// 혼잡도
			String congestion =
				cityData
					.path("LIVE_PPLTN_STTS")
					.path("AREA_CONGEST_LVL")
					.asText("정보 없음");

			// 기온
			String temp =
				cityData
					.path("WEATHER_STTS")
					.path("TEMP")
					.asText("정보 없음");

			// 날씨
			String sky =
				cityData
					.path("WEATHER_STTS")
					.path("SKY_STTS")
					.asText("정보 없음");

			return """
				현재 혼잡도는 %s 상태입니다.
				현재 기온은 %s도입니다.
				현재 날씨는 %s 입니다.
				""".formatted(
				congestion,
				temp,
				sky
			);
		} catch (Exception e) {
			e.printStackTrace();
			return """
				도시 환경 분석 실패
				""";
		}
	}
}
