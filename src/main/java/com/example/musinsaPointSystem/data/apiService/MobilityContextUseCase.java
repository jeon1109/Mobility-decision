package com.example.musinsaPointSystem.data.apiService;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.data.evidence.EvidenceType;
import com.example.musinsaPointSystem.data.evidence.FreshnessPolicy;
import com.example.musinsaPointSystem.data.evidence.FreshnessStatus;
import com.example.musinsaPointSystem.data.traffic.TrafficDataProvider;
import com.example.musinsaPointSystem.dto.SeoulArea;
import com.example.musinsaPointSystem.dto.mobility.CitySituation;
import com.example.musinsaPointSystem.dto.mobility.MobilityContextResponse;
import com.example.musinsaPointSystem.dto.mobility.MobilityContextResponse.CongestionEvidence;
import com.example.musinsaPointSystem.dto.mobility.MobilityContextResponse.Origin;
import com.example.musinsaPointSystem.dto.mobility.MobilityContextResponse.Situation;
import com.example.musinsaPointSystem.dto.mobility.MobilityContextResponse.SubwayEvidence;
import com.example.musinsaPointSystem.dto.mobility.MobilityContextResponse.SubwayStation;
import com.example.musinsaPointSystem.dto.mobility.MobilityContextResponse.TrafficEvidence;
import com.example.musinsaPointSystem.dto.mobility.MobilityContextResponse.WeatherEvidence;

@Service
public class MobilityContextUseCase {
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final SeoulAreaService areaService;
	private final TrafficDataProvider trafficDataProvider;
	private final FreshnessPolicy freshnessPolicy;
	private final Clock clock;

	public MobilityContextUseCase(SeoulAreaService areaService,
		TrafficDataProvider trafficDataProvider, FreshnessPolicy freshnessPolicy, Clock clock) {
		this.areaService = areaService;
		this.trafficDataProvider = trafficDataProvider;
		this.freshnessPolicy = freshnessPolicy;
		this.clock = clock;
	}

	public MobilityContextResponse getCurrentContext(String originAreaCode) {
		SeoulArea area = areaService.requireArea(originAreaCode);
		CitySituation city = trafficDataProvider.getCitySituation(area.areaCode(), area.areaName());
		String collectedAt = ZonedDateTime.now(clock).withZoneSameInstant(SEOUL)
			.toOffsetDateTime().toString();

		CitySituation.Weather weather = city.weather();
		CitySituation.Congestion congestion = city.congestion();
		CitySituation.RoadTraffic traffic = city.roadTraffic();
		CitySituation.NearbySnapshot subway = city.nearbyMobility().subwayStations();

		FreshnessStatus weatherFreshness = freshnessPolicy.evaluate(weather.observedAt(), EvidenceType.WEATHER);
		FreshnessStatus congestionFreshness = freshnessPolicy.evaluate(congestion.observedAt(), EvidenceType.CONGESTION);
		FreshnessStatus trafficFreshness = freshnessPolicy.evaluate(traffic.observedAt(), EvidenceType.TRAFFIC);
		FreshnessStatus subwayFreshness = freshnessPolicy.evaluate(subway.observedAt(), EvidenceType.SUBWAY);

		return new MobilityContextResponse(
			"1.1",
			collectedAt,
			new Origin(area.areaCode(), area.areaName()),
			new Situation(
				new WeatherEvidence(weather.status(), weather.temperatureCelsius(), weather.condition(),
					weather.precipitationType(), weather.humidityPercent(), weather.observedAt(),
					weather.source(), collectedAt, weatherFreshness),
				new CongestionEvidence(congestion.status(), congestion.level(), congestion.message(),
					congestion.populationMin(), congestion.populationMax(), congestion.observedAt(),
					congestion.source(), collectedAt, congestionFreshness),
				new TrafficEvidence(traffic.status(), traffic.level(), traffic.averageSpeedKph(),
					traffic.message(), traffic.observedAt(), traffic.source(), collectedAt, trafficFreshness),
				new SubwayEvidence(subway.status(), subway.count(), subway.observedAt(), subway.source(),
					collectedAt, subwayFreshness, subway.places().stream()
						.map(place -> new SubwayStation(place.name())).toList())
			),
			warningFor(List.of(weatherFreshness, congestionFreshness, trafficFreshness, subwayFreshness))
		);
	}

	private String warningFor(List<FreshnessStatus> statuses) {
		boolean stale = statuses.contains(FreshnessStatus.STALE);
		boolean unknown = statuses.contains(FreshnessStatus.UNKNOWN);
		if (stale && unknown) {
			return "일부 정보가 오래되었거나 데이터 기준시각을 확인할 수 없습니다.";
		}
		if (stale) {
			return "일부 정보는 최신 정보가 아닐 수 있습니다.";
		}
		if (unknown) {
			return "일부 정보의 데이터 기준시각을 확인할 수 없습니다.";
		}
		return null;
	}
}
