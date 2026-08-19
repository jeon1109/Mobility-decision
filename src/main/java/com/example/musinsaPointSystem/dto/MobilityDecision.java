package com.example.musinsaPointSystem.dto;

import java.util.List;

public record MobilityDecision(
        String recommendation,
        String reason,
        List<String> usedData,
        List<String> priority,
        String warning,
        WeatherSnapshot weather
) {
    public MobilityDecision withWeather(WeatherSnapshot currentWeather) {
        return new MobilityDecision(
                recommendation,
                reason,
                usedData,
                priority,
                warning,
                currentWeather
        );
    }

    public MobilityDecision withoutWeather() {
        return withWeather(null);
    }

    public static MobilityDecision fallback() {
        return new MobilityDecision(
                "환승이 적은 이동 전략",
                "현재 상황에서는 편안한 이동이 더 적합합니다.",
                List.of("서울시 실시간 도시데이터"),
                List.of("편안함", "혼잡 회피"),
                "실시간 교통 상황을 확인하세요.",
                WeatherSnapshot.unavailable()
        );
    }
}
