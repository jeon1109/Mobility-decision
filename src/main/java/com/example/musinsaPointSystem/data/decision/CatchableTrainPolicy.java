package com.example.musinsaPointSystem.data.decision;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.musinsaPointSystem.data.decision.model.CatchableTrain;
import com.example.musinsaPointSystem.data.decision.model.NearbyStation;
import com.example.musinsaPointSystem.data.decision.model.SubwayArrivalEvidence;

@Component
public class CatchableTrainPolicy {
	private final int safetyBufferSeconds;

	@Autowired
	public CatchableTrainPolicy(@Value("${mobility.catchable-train.safety-buffer:120s}") java.time.Duration buffer) {
		this.safetyBufferSeconds = Math.toIntExact(buffer.toSeconds());
	}

	CatchableTrainPolicy(int safetyBufferSeconds) {
		this.safetyBufferSeconds = safetyBufferSeconds;
	}

	public Optional<CatchableTrain> find(NearbyStation station, List<SubwayArrivalEvidence> arrivals) {
		int required = value(station.walkingDurationSeconds()) + safetyBufferSeconds;
		return arrivals.stream()
			.filter(value -> value.arrivalSeconds() != null && value.arrivalSeconds() >= required)
			.min(Comparator.comparingInt(SubwayArrivalEvidence::arrivalSeconds))
			.map(value -> new CatchableTrain(station.stationName(), value.line(), value.direction(),
				value.arrivalSeconds(), true, "역 이동시간과 안전 여유시간을 반영했습니다."));
	}

	private int value(Integer value) {
		return value == null ? Integer.MAX_VALUE / 2 : value;
	}
}
