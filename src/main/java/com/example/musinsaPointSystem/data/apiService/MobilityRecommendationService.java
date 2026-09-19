package com.example.musinsaPointSystem.data.apiService;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.MDC;

import com.example.musinsaPointSystem.data.traffic.TrafficDataProvider;
import com.example.musinsaPointSystem.data.location.port.CityDataAreaResolver;
import com.example.musinsaPointSystem.dto.SeoulArea;
import com.example.musinsaPointSystem.dto.mobility.CitySituation;
import com.example.musinsaPointSystem.dto.mobility.CitySituation.CongestionForecast;
import com.example.musinsaPointSystem.dto.mobility.CitySituation.DataStatus;
import com.example.musinsaPointSystem.dto.mobility.MobilityFlowRequest;
import com.example.musinsaPointSystem.dto.mobility.MobilityRequest;
import com.example.musinsaPointSystem.dto.mobility.MobilityRequest.Preference;
import com.example.musinsaPointSystem.dto.mobility.MobilityRequest.TransportMode;
import com.example.musinsaPointSystem.dto.mobility.MobilityRequest.UserState;
import com.example.musinsaPointSystem.dto.mobility.MobilityResponse;
import com.example.musinsaPointSystem.dto.mobility.MobilityResponse.Area;
import com.example.musinsaPointSystem.dto.mobility.MobilityResponse.Evidence;
import com.example.musinsaPointSystem.dto.mobility.MobilityResponse.EvidenceType;
import com.example.musinsaPointSystem.dto.mobility.MobilityResponse.Forecasts;
import com.example.musinsaPointSystem.dto.mobility.MobilityResponse.Guidance;
import com.example.musinsaPointSystem.dto.mobility.MobilityResponse.Limitation;
import com.example.musinsaPointSystem.dto.mobility.MobilityResponse.LimitationCode;
import com.example.musinsaPointSystem.dto.mobility.MobilityResponse.Situation;
import com.example.musinsaPointSystem.dto.mobility.MobilityResponse.StrategyCode;
import com.example.musinsaPointSystem.performance.MobilityPerformanceMetrics;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MobilityRecommendationService {
	private final TrafficDataProvider trafficDataProvider;
	private final SeoulAreaService areaService;
	private final MobilityPerformanceMetrics metrics;
	private final Clock clock;
	private final CityDataAreaResolver areaResolver;
	private final MobilityAgentService mobilityAgentService;

	@Autowired
	public MobilityRecommendationService(TrafficDataProvider trafficDataProvider, SeoulAreaService areaService,
		MobilityPerformanceMetrics metrics, Clock clock, CityDataAreaResolver areaResolver,
		MobilityAgentService mobilityAgentService) {
		this.trafficDataProvider = trafficDataProvider;
		this.areaService = areaService;
		this.metrics = metrics;
		this.clock = clock;
		this.areaResolver = areaResolver;
		this.mobilityAgentService = mobilityAgentService;
	}

	MobilityRecommendationService(TrafficDataProvider trafficDataProvider, SeoulAreaService areaService,
		MobilityPerformanceMetrics metrics, Clock clock) {
		this(trafficDataProvider, areaService, metrics, clock, null, null);
	}

	public MobilityResponse recommend(MobilityRequest request) {
		log.info("correlationId={} operation=mobility-recommendation recommendationEngine=RULE_BASED "
			+ "aiModelUsed=false areaCode={}", MDC.get("correlationId"), request.areaCode());
		SeoulArea area = areaService.requireArea(request.areaCode());
		CitySituation city = trafficDataProvider.getCitySituation(area.areaCode(), area.areaName());
		MobilityResponse response = metrics.record("decision.policy", () -> build(request, area, city));
		log.info("correlationId={} operation=mobility-recommendation recommendationEngine=RULE_BASED "
			+ "aiModelUsed=false outcome=success strategy={} recommendedMode={}",
			MDC.get("correlationId"), response.guidance().strategyCode(),
			response.guidance().recommendedMode());
		return response;
	}

	public MobilityResponse recommend(MobilityFlowRequest request) {
		MobilityRequest compatibleRequest = new MobilityRequest(
			request.originAreaCode(), request.purpose(), List.of(), request.toPreferences());
		SeoulArea area = resolveArea(request);
		CitySituation city = trafficDataProvider.getCitySituation(area.areaCode(), area.areaName());
		MobilityResponse response = build(compatibleRequest, area, city);
		Guidance guidance = response.guidance();
		Guidance updatedGuidance = new Guidance(
			guidance.strategyCode(), guidance.recommendedMode(), guidance.headline(),
			"목적지 '%s', 우선순위 '%s'를 반영했습니다. %s".formatted(request.destination(),
				String.join(", ", request.priorities().stream().map(priority -> priority.label()).toList()),
				guidance.summary()),
			guidance.evidence(), guidance.limitations());
		List<String> updatedPriorities = new ArrayList<>(response.priorities());
		updatedPriorities.add("목적지: " + request.destination());
		MobilityResponse updated = new MobilityResponse(response.schemaVersion(), response.generatedAt(), response.area(),
			updatedGuidance, response.situation(), response.forecasts(), response.alerts(),
			response.nearbyMobility(), List.copyOf(updatedPriorities), response.warning());
		return withAiAnalysis(request, area, city, updated);
	}

	private SeoulArea resolveArea(MobilityFlowRequest request) {
		if (request.origin() != null && areaResolver != null) {
			return areaResolver.resolve(request.origin())
				.orElseThrow(() -> new IllegalArgumentException(
					"선택한 출발지는 현재 서울 실시간 도시정보 지원 지역과 연결되지 않습니다."));
		}
		return areaService.requireArea(request.originAreaCode());
	}

	private MobilityResponse withAiAnalysis(MobilityFlowRequest request, SeoulArea area,
		CitySituation city, MobilityResponse response) {
		if (mobilityAgentService == null || request.origin() == null || request.destinationLocation() == null) {
			return response;
		}
		try {
			String analysis = mobilityAgentService.analyze(request, area, city, response.guidance().summary());
			Guidance guidance = response.guidance();
			Guidance analyzed = new Guidance(guidance.strategyCode(), guidance.recommendedMode(),
				guidance.headline(), analysis, guidance.evidence(), guidance.limitations());
			return new MobilityResponse(response.schemaVersion(), response.generatedAt(), response.area(),
				analyzed, response.situation(), response.forecasts(), response.alerts(),
				response.nearbyMobility(), response.priorities(), response.warning());
		} catch (RuntimeException e) {
			log.warn("correlationId={} operation=mobility-detail-analysis outcome=fallback errorType={}",
				MDC.get("correlationId"), e.getClass().getSimpleName());
			return response;
		}
	}

	private MobilityResponse build(MobilityRequest request, SeoulArea area, CitySituation city) {
		TransportMode mode = chooseMode(request, city);
		CrowdAdvice crowdAdvice = crowdAdvice(request, city);
		StrategyCode strategy = crowdAdvice == null
			? mode == null ? StrategyCode.INSUFFICIENT_DATA : StrategyCode.consider(mode)
			: crowdAdvice.strategyCode();
		List<Evidence> evidence = evidence(request, city, mode, crowdAdvice);
		Guidance guidance = new Guidance(strategy, mode,
			headline(mode, crowdAdvice),
			summary(request, city, mode, crowdAdvice), evidence,
			limitations(request, city));
		return new MobilityResponse("1.0",
			ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime().toString(),
			new Area(area.areaCode(), area.areaName()), guidance,
			new Situation(city.congestion(), city.weather(), city.roadTraffic(), city.airQuality()),
			new Forecasts(city.weatherForecasts(), city.congestionForecasts()), List.of(),
			city.nearbyMobility(), priorities(request, mode),
			"실시간 관측은 실제 현장과 차이가 있을 수 있으므로 출발 전에 다시 확인해주세요.");
	}

	private List<Evidence> evidence(MobilityRequest request, CitySituation city,
		TransportMode mode, CrowdAdvice crowdAdvice) {
		List<Evidence> evidence = new ArrayList<>();
		if (city.congestion().status() != DataStatus.UNAVAILABLE) {
			evidence.add(new Evidence(EvidenceType.OBSERVATION, List.of("congestion.current"),
				congestionMessage(city)));
		}
		if (city.roadTraffic().status() == DataStatus.LIVE) {
			evidence.add(new Evidence(EvidenceType.OBSERVATION, List.of("roadTraffic.current"),
				"도로 관측 상태는 %s입니다.".formatted(city.roadTraffic().level())));
		}
		if (weatherExposureRisk(city)) {
			evidence.add(new Evidence(EvidenceType.OBSERVATION, List.of("weather.current"),
				"현재 날씨 관측상 비·눈 노출 가능성이 있습니다."));
		}
		for (UserState state : request.userStates()) {
			evidence.add(new Evidence(EvidenceType.USER_STATE, List.of("user.states." + state.name()),
				"사용자 상태 '%s' 항목을 이동수단 판단에 반영했습니다.".formatted(state.label())));
		}
		for (Preference preference : request.preferences()) {
			evidence.add(new Evidence(EvidenceType.USER_PREFERENCE, List.of("user.preferences." + preference.name()),
				"사용자가 '%s' 항목을 선택했습니다.".formatted(preference.label())));
		}
		if (crowdAdvice != null) {
			evidence.add(new Evidence(EvidenceType.SYSTEM_JUDGMENT, crowdAdvice.factIds(), crowdAdvice.message()));
		}
		evidence.add(new Evidence(EvidenceType.SYSTEM_JUDGMENT, List.of(), mode == null
			? "사용자 상태와 이용 가능한 수단을 함께 고려했을 때 안전하게 제안할 이동수단이 없습니다."
			: "'%s' 이동을 우선 고려하도록 규칙 기반으로 판단했습니다.".formatted(mode.label())));
		return List.copyOf(evidence);
	}

	private String congestionMessage(CitySituation city) {
		Integer min = city.congestion().populationMin();
		Integer max = city.congestion().populationMax();
		if (min != null && max != null) {
			return "지역 혼잡 관측은 %s이며 추정 인구는 %,d~%,d명입니다."
				.formatted(city.congestion().level(), min, max);
		}
		return "지역 혼잡 관측 상태는 %s입니다.".formatted(city.congestion().level());
	}

	private TransportMode chooseMode(MobilityRequest request, CitySituation city) {
		List<TransportMode> allowed = new ArrayList<>(List.of(TransportMode.values()));
		applySafetyConstraints(allowed, request.userStates());
		if (allowed.isEmpty()) return null;

		if (request.userStates().contains(UserState.LATE_NIGHT)) {
			TransportMode lateNight = firstAllowed(allowed, TransportMode.TAXI, TransportMode.CAR, TransportMode.EV);
			if (lateNight != null) return lateNight;
		}
		if (request.preferences().contains(Preference.LESS_WALKING)) {
			TransportMode lessWalking = firstAllowed(allowed, TransportMode.TAXI,
				TransportMode.CAR, TransportMode.PUBLIC_TRANSIT, TransportMode.EV);
			if (lessWalking != null) return lessWalking;
		}
		if (request.preferences().contains(Preference.LOW_COST)
			&& allowed.contains(TransportMode.PUBLIC_TRANSIT)) {
			return TransportMode.PUBLIC_TRANSIT;
		}
		if (request.preferences().contains(Preference.FAST)) {
			TransportMode fast = isSlowRoad(city)
				? firstAllowed(allowed, TransportMode.PUBLIC_TRANSIT, TransportMode.TAXI)
				: firstAllowed(allowed, TransportMode.TAXI, TransportMode.PUBLIC_TRANSIT);
			if (fast != null) return fast;
		}
		if (request.preferences().contains(Preference.MINIMIZE_WEATHER_EXPOSURE)
			&& weatherExposureRisk(city)) {
			TransportMode covered = firstAllowed(allowed, TransportMode.PUBLIC_TRANSIT,
				TransportMode.TAXI, TransportMode.CAR, TransportMode.EV);
			if (covered != null) return covered;
		}
		if (request.userStates().stream().anyMatch(this::needsLowEffortMode)) {
			TransportMode lowEffort = firstAllowed(allowed, TransportMode.PUBLIC_TRANSIT,
				TransportMode.TAXI, TransportMode.CAR, TransportMode.EV);
			if (lowEffort != null) return lowEffort;
		}
		if ((request.preferences().contains(Preference.AVOID_ROAD_DELAY) || isSlowRoad(city))
			&& allowed.contains(TransportMode.PUBLIC_TRANSIT)) {
			return TransportMode.PUBLIC_TRANSIT;
		}
		return allowed.contains(TransportMode.PUBLIC_TRANSIT)
			? TransportMode.PUBLIC_TRANSIT : allowed.getFirst();
	}

	private void applySafetyConstraints(List<TransportMode> allowed, List<UserState> states) {
		if (states.contains(UserState.LIMITED_WALKING)) {
			allowed.remove(TransportMode.WALK);
			allowed.remove(TransportMode.BIKE);
		}
		if (states.contains(UserState.CARRYING_LUGGAGE) || states.contains(UserState.WITH_CHILD)) {
			allowed.remove(TransportMode.BIKE);
		}
	}

	private boolean needsLowEffortMode(UserState state) {
		return state == UserState.TIRED || state == UserState.CARRYING_LUGGAGE
			|| state == UserState.LIMITED_WALKING || state == UserState.WITH_CHILD;
	}

	private TransportMode firstAllowed(List<TransportMode> allowed, TransportMode... candidates) {
		for (TransportMode candidate : candidates) {
			if (allowed.contains(candidate)) return candidate;
		}
		return null;
	}

	private CrowdAdvice crowdAdvice(MobilityRequest request, CitySituation city) {
		if (!request.preferences().contains(Preference.AVOID_CROWD)
			|| city.congestion().status() != DataStatus.LIVE
			|| crowdRank(city.congestion().level()) < crowdRank("약간 붐빔")) {
			return null;
		}
		int currentRank = crowdRank(city.congestion().level());
		CongestionForecast quieter = city.congestionForecasts().stream()
			.filter(forecast -> crowdRank(forecast.level()) < currentRank)
			.findFirst().orElse(null);
		if (quieter != null) {
			return new CrowdAdvice(StrategyCode.CONSIDER_DELAYED_DEPARTURE,
				"현재보다 혼잡도가 낮아질 것으로 보이는 %s 이후 출발을 고려하세요."
					.formatted(quieter.forecastAt()),
				List.of("congestion.current", "congestion.forecast"));
		}
		return new CrowdAdvice(StrategyCode.CAUTION_CROWD,
			"현재 혼잡도가 높고 더 한산해지는 시각을 확인할 수 없어 출발 직전에 다시 확인하세요.",
			List.of("congestion.current"));
	}

	private int crowdRank(String level) {
		if (!StringUtils.hasText(level)) return -1;
		if (level.contains("붐빔") && !level.contains("약간")) return 3;
		if (level.contains("약간 붐빔")) return 2;
		if (level.contains("보통")) return 1;
		if (level.contains("여유")) return 0;
		return -1;
	}

	private boolean weatherExposureRisk(CitySituation city) {
		if (city.weather().status() != DataStatus.LIVE) return false;
		String precipitation = city.weather().precipitationType();
		String condition = city.weather().condition();
		boolean precipitationObserved = StringUtils.hasText(precipitation)
			&& !precipitation.contains("없음") && !"0".equals(precipitation.trim());
		boolean conditionSuggestsPrecipitation = StringUtils.hasText(condition)
			&& (condition.contains("비") || condition.contains("눈"));
		return precipitationObserved || conditionSuggestsPrecipitation;
	}


	private boolean isSlowRoad(CitySituation city) {
		String level = city.roadTraffic().level();
		return city.roadTraffic().status() == DataStatus.LIVE && level != null
			&& (level.contains("서행") || level.contains("정체"));
	}

	private String summary(MobilityRequest request, CitySituation city,
		TransportMode mode, CrowdAdvice crowdAdvice) {
		List<String> context = new ArrayList<>();
		context.add("이동 목적 '%s'".formatted(request.purpose().label()));
		request.userStates().stream().findFirst()
			.ifPresent(state -> context.add("사용자 상태 '%s'".formatted(state.label())));
		if (city.congestion().status() == DataStatus.LIVE && city.congestion().level() != null) {
			context.add("혼잡도 '%s'".formatted(city.congestion().level()));
		}
		if (city.roadTraffic().status() == DataStatus.LIVE && city.roadTraffic().level() != null) {
			context.add("도로 '%s'".formatted(city.roadTraffic().level()));
		}
		if (weatherExposureRisk(city)) {
			context.add("비·눈 노출 가능성");
		}
		String conclusion = mode == null
			? "현재 이용 가능한 수단 중 안전하게 우선 제안할 수단이 없습니다."
			: "'%s' 이동을 우선 고려할 수 있습니다.".formatted(mode.label());
		if (crowdAdvice != null) {
			conclusion += " " + crowdAdvice.message();
		}
		return "%s 정보를 반영했습니다. %s".formatted(String.join(", ", context), conclusion);
	}

	private String headline(TransportMode mode, CrowdAdvice crowdAdvice) {
		if (crowdAdvice != null) return crowdAdvice.message();
		if (mode == null) return "현재 조건에서는 안전하게 제안할 이동수단이 없습니다.";
		return switch (mode) {
			case PUBLIC_TRANSIT -> "대중교통 이용을 우선 고려해보세요.";
			case WALK -> "도보 이동을 고려해보세요.";
			case BIKE -> "자전거 이동을 고려해보세요.";
			case CAR -> "자동차 이동을 고려해보세요.";
			case TAXI -> "택시 이동을 고려해보세요.";
			case EV -> "전기차 이동을 고려해보세요.";
		};
	}

	private List<Limitation> limitations(MobilityRequest request, CitySituation city) {
		List<Limitation> values = new ArrayList<>();
		values.add(new Limitation(LimitationCode.ROUTE_DATA_NOT_AVAILABLE,
			"실제 노선과 소요시간은 별도 길찾기 서비스에서 확인해야 합니다."));
		if (request.userStates().contains(UserState.LATE_NIGHT)) {
			values.add(new Limitation(LimitationCode.TRANSIT_AVAILABILITY_NOT_VERIFIED,
				"심야 대중교통 운행 여부는 현재 데이터만으로 확인할 수 없습니다."));
		}
		if (city.congestion().status() == DataStatus.STALE) {
			values.add(new Limitation(LimitationCode.CROWD_DATA_STALE, "혼잡 관측 시각이 오래되어 현재 상황과 다를 수 있습니다."));
		} else if (city.congestion().status() == DataStatus.UNAVAILABLE) {
			values.add(new Limitation(LimitationCode.CROWD_DATA_UNAVAILABLE, "현재 혼잡도와 추정 인구를 확인할 수 없습니다."));
		}
		if (city.roadTraffic().status() == DataStatus.STALE) {
			values.add(new Limitation(LimitationCode.TRAFFIC_DATA_STALE,
				"도로 관측 시각이 오래되어 추천 판단 근거에서 제외했습니다."));
		} else if (city.roadTraffic().status() == DataStatus.UNAVAILABLE) {
			values.add(new Limitation(LimitationCode.TRAFFIC_DATA_UNAVAILABLE,
				"현재 도로 교통정보를 확인할 수 없습니다."));
		}
		return List.copyOf(values);
	}

	private List<String> priorities(MobilityRequest request, TransportMode mode) {
		List<String> values = new ArrayList<>();
		if (mode != null) values.add("추천 수단: " + mode.label());
		values.add("이동 목적: " + request.purpose().label());
		request.userStates().stream().map(state -> "사용자 상태: " + state.label()).forEach(values::add);
		request.preferences().stream().map(preference -> "사용자 선택: " + preference.label())
			.forEach(values::add);
		return List.copyOf(values);
	}

	private record CrowdAdvice(StrategyCode strategyCode, String message, List<String> factIds) {}
}
