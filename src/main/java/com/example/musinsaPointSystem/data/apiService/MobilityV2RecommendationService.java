package com.example.musinsaPointSystem.data.apiService;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.dto.SeoulArea;
import com.example.musinsaPointSystem.dto.mobilityv2.CitySituation;
import com.example.musinsaPointSystem.dto.mobilityv2.CitySituation.DataStatus;
import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Request;
import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Request.Preference;
import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Request.TransportMode;
import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Response;
import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Response.Area;
import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Response.Evidence;
import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Response.Forecasts;
import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Response.Guidance;
import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Response.Limitation;
import com.example.musinsaPointSystem.dto.mobilityv2.MobilityV2Response.Situation;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;

@Service
public class MobilityV2RecommendationService {
	private final ApiService apiService;
	private final SeoulAreaService areaService;
	private final MobilityPerformanceMetrics metrics;
	private final Clock clock;

	public MobilityV2RecommendationService(ApiService apiService, SeoulAreaService areaService,
		MobilityPerformanceMetrics metrics, Clock clock) {
		this.apiService = apiService;
		this.areaService = areaService;
		this.metrics = metrics;
		this.clock = clock;
	}

	public MobilityV2Response recommend(MobilityV2Request request) {
		if (request.availableModes() != null && request.availableModes().isEmpty()) {
			throw new InvalidMobilityV2RequestException("availableModes는 생략하거나 한 개 이상 선택해야 합니다.");
		}
		SeoulArea area = areaService.requireArea(request.areaCode());
		CitySituation city = apiService.getCitySituation(area.areaCode(), area.areaName());
		return metrics.record("decision.policy", () -> build(request, area, city));
	}

	private MobilityV2Response build(MobilityV2Request request, SeoulArea area, CitySituation city) {
		TransportMode mode = chooseMode(request, city);
		String strategy = mode == null ? "INSUFFICIENT_DATA" : "CONSIDER_" + mode.name();
		List<Evidence> evidence = new ArrayList<>();
		if (city.roadTraffic().status() != DataStatus.UNAVAILABLE) {
			evidence.add(new Evidence("OBSERVATION", List.of("roadTraffic.current"),
				"도로 관측 상태는 %s입니다.".formatted(city.roadTraffic().level())));
		} else if (city.congestion().status() != DataStatus.UNAVAILABLE) {
			evidence.add(new Evidence("OBSERVATION", List.of("congestion.current"),
				"지역 혼잡 관측 상태는 %s입니다.".formatted(city.congestion().level())));
		}
		for (Preference preference : request.preferences()) {
			evidence.add(new Evidence("USER_PREFERENCE", List.of("user.preferences." + preference.name()),
				"사용자가 %s 선호를 선택했습니다.".formatted(preference.name())));
		}
		evidence.add(new Evidence("SYSTEM_JUDGMENT", List.of(), mode == null
			? "사용 가능한 관측 정보가 부족하여 특정 수단을 제안하지 않았습니다."
			: "%s 수단을 우선 고려하도록 규칙 기반으로 판단했습니다.".formatted(mode.name())));

		Guidance guidance = new Guidance(strategy, mode,
			mode == null ? "현재 정보만으로 이동수단을 정하기 어렵습니다."
				: headline(mode),
			"실제 관측과 사용자의 선택을 함께 확인한 안내입니다.", evidence,
			List.of(new Limitation("ROUTE_DATA_NOT_AVAILABLE",
				"실제 노선과 소요시간은 별도 길찾기 서비스에서 확인해야 합니다.")));
		return new MobilityV2Response("2.0",
			ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime().toString(),
			new Area(area.areaCode(), area.areaName()), guidance,
			new Situation(city.congestion(), city.weather(), city.roadTraffic(), city.airQuality()),
			new Forecasts(city.weatherForecasts(), city.congestionForecasts()), List.of(),
			city.nearbyMobility(), priorities(request, mode),
			"실시간 상황은 달라질 수 있으므로 출발 전 지도 앱에서 확인해주세요.");
	}

	private TransportMode chooseMode(MobilityV2Request request, CitySituation city) {
		List<TransportMode> allowed = request.availableModes() == null
			? List.of(TransportMode.values()) : request.availableModes();
		if ((request.preferences().contains(Preference.AVOID_ROAD_DELAY)
			|| isSlowRoad(city)) && allowed.contains(TransportMode.PUBLIC_TRANSIT)) {
			return TransportMode.PUBLIC_TRANSIT;
		}
		if (request.preferences().contains(Preference.PREFER_PUBLIC_TRANSIT)
			&& allowed.contains(TransportMode.PUBLIC_TRANSIT)) return TransportMode.PUBLIC_TRANSIT;
		if (request.preferences().contains(Preference.PREFER_BIKE)
			&& allowed.contains(TransportMode.BIKE)) return TransportMode.BIKE;
		if (request.preferences().contains(Preference.PREFER_CAR)
			&& allowed.contains(TransportMode.CAR)) return TransportMode.CAR;
		return allowed.contains(TransportMode.PUBLIC_TRANSIT) ? TransportMode.PUBLIC_TRANSIT
			: allowed.stream().findFirst().orElse(null);
	}

	private boolean isSlowRoad(CitySituation city) {
		String level = city.roadTraffic().level();
		return city.roadTraffic().status() != DataStatus.UNAVAILABLE && level != null
			&& (level.contains("서행") || level.contains("정체"));
	}

	private String headline(TransportMode mode) {
		return switch (mode) {
			case PUBLIC_TRANSIT -> "대중교통 이용을 우선 고려해보세요.";
			case WALK -> "도보 이동을 고려해보세요.";
			case BIKE -> "자전거 이동을 고려해보세요.";
			case CAR -> "자동차 이동을 고려해보세요.";
			case TAXI -> "택시 이동을 고려해보세요.";
			case EV -> "전기차 이동을 고려해보세요.";
		};
	}

	private List<String> priorities(MobilityV2Request request, TransportMode mode) {
		List<String> values = new ArrayList<>();
		if (mode != null) values.add(mode.name());
		request.preferences().stream().map(Enum::name).forEach(values::add);
		return List.copyOf(values);
	}

	public static class InvalidMobilityV2RequestException extends RuntimeException {
		public InvalidMobilityV2RequestException(String message) { super(message); }
	}
}
