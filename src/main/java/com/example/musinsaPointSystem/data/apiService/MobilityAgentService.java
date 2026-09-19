package com.example.musinsaPointSystem.data.apiService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.slf4j.MDC;

import com.example.musinsaPointSystem.common.error.AiCommunicationException;
import com.example.musinsaPointSystem.dto.MobilityContext;
import com.example.musinsaPointSystem.dto.SeoulArea;
import com.example.musinsaPointSystem.dto.mobility.CitySituation;
import com.example.musinsaPointSystem.dto.mobility.MobilityFlowRequest;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilityAgentService {
	private final ChatClient chatClient;
	private final MobilityPerformanceMetrics metrics;

	public MobilityAgentService(ChatClient.Builder builder, MobilityPerformanceMetrics metrics) {
		this.chatClient = builder.build();
		this.metrics = metrics;
	}

	public String recommend(MobilityContext context, String cityDataSummary) {
		String system = """
			당신은 상황 기반 이동 의사결정 AI입니다. 제공된 관측 데이터 외의 날씨, 경로,
			거리, 소요시간을 추정하지 마세요. 반드시 recommendation, reason, usedData,
			priority, warning 필드를 갖는 JSON 객체만 출력하세요.
			""";
		String user = "사용자 상태: %s\n이동 목적: %s\n현재 지역: %s\n관측 데이터:\n%s"
			.formatted(context.condition(), context.purpose(), context.areaName(), cityDataSummary);
		try {
			ChatResponse response = metrics.record("spring-ai.total", () -> chatClient.prompt()
				.system(system).user(user).call().chatResponse());
			Usage usage = response.getMetadata().getUsage();
			log.info("correlationId={} operation=mobility-recommendation recommendationEngine=SPRING_AI "
				+ "aiModelUsed=true outcome=success model={} totalTokens={}",
				MDC.get("correlationId"), response.getMetadata().getModel(),
				usage == null ? "unavailable" : usage.getTotalTokens());
			return response.getResult().getOutput().getText();
		} catch (RuntimeException e) {
			log.warn("correlationId={} operation=mobility-recommendation recommendationEngine=SPRING_AI "
				+ "aiModelUsed=true outcome=error errorType={}", MDC.get("correlationId"),
				e.getClass().getSimpleName());
			throw new AiCommunicationException("AI 안내 생성에 실패했습니다.", e);
		}
	}

	public String analyze(MobilityFlowRequest request, SeoulArea area, CitySituation city,
		String ruleBasedSummary) {
		String system = """
			당신은 검증된 위치와 관측 Evidence를 설명하는 이동 의사결정 AI입니다.
			장소, 주소, 좌표, 행정구역, 노선, 거리, 소요시간을 새로 추측하지 마세요.
			규칙 엔진이 선택한 이동수단을 변경하지 말고, 제공된 사실이 선택에 어떤 영향을
			주는지 한국어 3~5문장으로 구체적으로 설명하세요. 데이터가 없으면 없다고 명시하세요.
			마크다운 제목이나 JSON 없이 설명문만 출력하세요.
			""";
		String subwayNames = city.nearbyMobility().subwayStations().places().stream()
			.map(CitySituation.NearbyPlace::name).limit(5).toList().toString();
		String user = """
			[검증된 위치]
			출발지: %s / %s / 위도 %s / 경도 %s / %s %s
			목적지: %s / %s / 위도 %s / 경도 %s / %s %s
			CITYDATA 기준지역: %s(%s)
			이동 목적: %s
			우선순위: %s

			[현재 Evidence]
			날씨: 상태=%s, 온도=%s, 하늘=%s, 강수=%s, 습도=%s, 관측=%s
			혼잡도: 상태=%s, 단계=%s, 인구=%s~%s, 메시지=%s, 관측=%s
			도로: 상태=%s, 단계=%s, 평균속도=%s, 메시지=%s, 관측=%s
			대기질: %s
			지하철: 상태=%s, 개수=%s, 역=%s

			[규칙 엔진 결과]
			%s
			""".formatted(
			locationValue(request.origin(), LocationField.NAME), locationValue(request.origin(), LocationField.ADDRESS),
			locationValue(request.origin(), LocationField.LATITUDE), locationValue(request.origin(), LocationField.LONGITUDE),
			locationValue(request.origin(), LocationField.CITY), locationValue(request.origin(), LocationField.DISTRICT),
			locationValue(request.destinationLocation(), LocationField.NAME), locationValue(request.destinationLocation(), LocationField.ADDRESS),
			locationValue(request.destinationLocation(), LocationField.LATITUDE), locationValue(request.destinationLocation(), LocationField.LONGITUDE),
			locationValue(request.destinationLocation(), LocationField.CITY), locationValue(request.destinationLocation(), LocationField.DISTRICT),
			area.areaName(), area.areaCode(), request.purpose().label(),
			request.priorities().stream().map(MobilityFlowRequest.Priority::label).toList(),
			city.weather().status(), city.weather().temperatureCelsius(), city.weather().condition(),
			city.weather().precipitationType(), city.weather().humidityPercent(), city.weather().observedAt(),
			city.congestion().status(), city.congestion().level(), city.congestion().populationMin(),
			city.congestion().populationMax(), city.congestion().message(), city.congestion().observedAt(),
			city.roadTraffic().status(), city.roadTraffic().level(), city.roadTraffic().averageSpeedKph(),
			city.roadTraffic().message(), city.roadTraffic().observedAt(), city.airQuality(),
			city.nearbyMobility().subwayStations().status(), city.nearbyMobility().subwayStations().count(),
			subwayNames, ruleBasedSummary);
		try {
			ChatResponse response = metrics.record("spring-ai.mobility-detail", () -> chatClient.prompt()
				.system(system).user(user).call().chatResponse());
			Usage usage = response.getMetadata().getUsage();
			log.info("correlationId={} operation=mobility-detail-analysis recommendationEngine=SPRING_AI "
				+ "aiModelUsed=true outcome=success model={} totalTokens={}", MDC.get("correlationId"),
				response.getMetadata().getModel(), usage == null ? "unavailable" : usage.getTotalTokens());
			String result = response.getResult().getOutput().getText();
			if (result == null || result.isBlank()) throw new AiCommunicationException("AI 상세분석이 비어 있습니다.");
			return result.trim();
		} catch (RuntimeException e) {
			log.warn("correlationId={} operation=mobility-detail-analysis recommendationEngine=SPRING_AI "
				+ "aiModelUsed=true outcome=error errorType={}", MDC.get("correlationId"),
				e.getClass().getSimpleName());
			throw new AiCommunicationException("AI 상세분석 생성에 실패했습니다.", e);
		}
	}

	private String locationValue(com.example.musinsaPointSystem.data.location.model.Location location,
		LocationField field) {
		if (location == null) return "미제공";
		Object value = switch (field) {
			case NAME -> location.name();
			case ADDRESS -> location.address();
			case LATITUDE -> location.latitude();
			case LONGITUDE -> location.longitude();
			case CITY -> location.city();
			case DISTRICT -> location.district();
		};
		return value == null ? "미제공" : value.toString();
	}

	private enum LocationField { NAME, ADDRESS, LATITUDE, LONGITUDE, CITY, DISTRICT }
}
