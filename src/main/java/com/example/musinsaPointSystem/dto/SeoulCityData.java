package com.example.musinsaPointSystem.dto;

public record SeoulCityData(
	String congestion,
	WeatherSnapshot weather
) {
	public static SeoulCityData unavailable() {
		return new SeoulCityData("정보 없음", WeatherSnapshot.unavailable());
	}

	public String toPromptSummary() {
		if (!weather.available()) {
			return "도시 데이터를 가져오지 못했습니다. 현재 혼잡도는 %s 상태입니다."
				.formatted(congestion);
		}

		String condition = weather.condition() == null ? "정보 없음" : weather.condition();
		return """
			현재 혼잡도는 %s 상태입니다.
			현재 기온은 %s도입니다.
			현재 날씨는 %s 입니다.
			관측 시각은 %s 입니다.
			""".formatted(
			congestion,
			weather.temperatureCelsius(),
			condition,
			weather.observedAt() == null ? "정보 없음" : weather.observedAt()
		);
	}
}
