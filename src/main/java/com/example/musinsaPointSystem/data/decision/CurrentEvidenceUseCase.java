package com.example.musinsaPointSystem.data.decision;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.springframework.stereotype.Service;

import com.example.musinsaPointSystem.data.decision.model.NearbyStation;
import com.example.musinsaPointSystem.data.decision.model.SubwayArrivalEvidence;
import com.example.musinsaPointSystem.data.decision.port.NearbyStationPort;
import com.example.musinsaPointSystem.data.decision.port.SubwayArrivalPort;
import com.example.musinsaPointSystem.data.location.model.Location;
import com.example.musinsaPointSystem.data.location.port.CityDataAreaResolver;
import com.example.musinsaPointSystem.data.traffic.TrafficDataProvider;
import com.example.musinsaPointSystem.dto.SeoulArea;
import com.example.musinsaPointSystem.dto.mobility.CitySituation;
import com.example.musinsaPointSystem.dto.mobility.CurrentEvidenceResponse;
import com.example.musinsaPointSystem.dto.mobility.CurrentEvidenceResponse.StationEvidence;

@Service
public class CurrentEvidenceUseCase {
	private final CityDataAreaResolver areaResolver;
	private final TrafficDataProvider trafficDataProvider;
	private final NearbyStationPort nearbyStationPort;
	private final SubwayArrivalPort subwayArrivalPort;
	private final CatchableTrainPolicy catchableTrainPolicy;
	private final ExecutorService executor;
	private final Clock clock;

	public CurrentEvidenceUseCase(CityDataAreaResolver areaResolver, TrafficDataProvider trafficDataProvider,
		NearbyStationPort nearbyStationPort, SubwayArrivalPort subwayArrivalPort,
		CatchableTrainPolicy catchableTrainPolicy, ExecutorService executor, Clock clock) {
		this.areaResolver = areaResolver; this.trafficDataProvider = trafficDataProvider;
		this.nearbyStationPort = nearbyStationPort; this.subwayArrivalPort = subwayArrivalPort;
		this.catchableTrainPolicy = catchableTrainPolicy; this.executor = executor; this.clock = clock;
	}

	public CurrentEvidenceResponse get(Location origin) {
		validate(origin);
		SeoulArea area = areaResolver.resolve(origin).orElse(null);
		CompletableFuture<CitySituation> cityFuture = CompletableFuture.supplyAsync(() -> area == null
			? CitySituation.unavailable(null, origin.name())
			: trafficDataProvider.getCitySituation(area.areaCode(), area.areaName()), executor);
		CompletableFuture<List<NearbyStation>> stationFuture = CompletableFuture.supplyAsync(
			() -> nearbyStationPort.findNearby(origin), executor);
		CitySituation city = cityFuture.join();
		List<NearbyStation> stations = stationFuture.join();
		List<CompletableFuture<StationEvidence>> stationEvidence = stations.stream().limit(3)
			.map(station -> CompletableFuture.supplyAsync(() -> stationEvidence(station), executor)).toList();
		List<StationEvidence> stationValues = stationEvidence.stream().map(CompletableFuture::join).toList();
		List<String> unavailable = new ArrayList<>();
		if (area == null) unavailable.add("SEOUL_CITY_DATA");
		if (stations.isEmpty()) unavailable.add("NEARBY_STATIONS");
		if (!stations.isEmpty() && stationValues.stream().allMatch(value -> value.arrivals().isEmpty()))
			unavailable.add("SUBWAY_ARRIVAL");
		return new CurrentEvidenceResponse("2.0", ZonedDateTime.now(clock)
			.withZoneSameInstant(ZoneId.of("Asia/Seoul")).toOffsetDateTime().toString(),
			origin, area, city, stationValues, unavailable);
	}

	private StationEvidence stationEvidence(NearbyStation station) {
		List<SubwayArrivalEvidence> arrivals = subwayArrivalPort.findArrivals(station.stationName()).stream()
			.filter(arrival -> SubwayNaming.sameLine(station.line(), arrival.line()))
			.toList();
		return new StationEvidence(station, arrivals,
			catchableTrainPolicy.find(station, arrivals).orElse(null));
	}

	private void validate(Location location) {
		if (location == null || location.latitude() == null || location.longitude() == null)
			throw new IllegalArgumentException("출발지 좌표가 필요합니다.");
		if (location.latitude().doubleValue() < -90 || location.latitude().doubleValue() > 90
			|| location.longitude().doubleValue() < -180 || location.longitude().doubleValue() > 180)
			throw new IllegalArgumentException("출발지 좌표가 올바르지 않습니다.");
	}
}
