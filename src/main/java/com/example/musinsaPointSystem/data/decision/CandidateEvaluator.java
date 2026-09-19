package com.example.musinsaPointSystem.data.decision;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.example.musinsaPointSystem.data.decision.model.DecisionPreference;
import com.example.musinsaPointSystem.data.decision.model.EvaluatedCandidate;
import com.example.musinsaPointSystem.data.decision.model.MobilityCandidate;
import com.example.musinsaPointSystem.dto.mobility.CitySituation;

@Component
public class CandidateEvaluator {
	public List<EvaluatedCandidate> evaluate(List<MobilityCandidate> candidates,
		List<DecisionPreference> preferences, CitySituation evidence) {
		if (candidates == null || candidates.isEmpty()) return List.of();
		return candidates.stream().map(candidate -> evaluate(candidate, candidates, preferences, evidence))
			.sorted(Comparator.comparingDouble(EvaluatedCandidate::score).reversed()).toList();
	}

	private EvaluatedCandidate evaluate(MobilityCandidate candidate, List<MobilityCandidate> all,
		List<DecisionPreference> preferences, CitySituation evidence) {
		double score = 50.0;
		List<String> insights = new ArrayList<>();
		List<String> risks = new ArrayList<>();
		List<DecisionPreference> matched = new ArrayList<>();

		for (DecisionPreference preference : preferences) {
			switch (preference) {
				case FAST -> score += rewardMinimum(candidate, all, MobilityCandidate::estimatedDurationSeconds,
					20, "이동시간이 가장 짧은 후보입니다.", insights, matched, preference);
				case LESS_WALKING -> score += rewardMinimum(candidate, all, MobilityCandidate::walkingDurationSeconds,
					18, "도보시간이 가장 짧은 후보입니다.", insights, matched, preference);
				case LOW_COST -> score += rewardMinimum(candidate, all, MobilityCandidate::estimatedCostWon,
					16, "확인 가능한 비용이 가장 낮은 후보입니다.", insights, matched, preference);
				case FEWER_TRANSFERS -> score += rewardMinimum(candidate, all, MobilityCandidate::transfers,
					14, "환승 횟수가 가장 적은 후보입니다.", insights, matched, preference);
				case AVOID_CONGESTION -> {
					if (isRoadMode(candidate) && isHeavyTraffic(evidence)) {
						score -= 22;
						risks.add("현재 도로 정체의 영향을 받을 수 있습니다.");
					} else if (!isRoadMode(candidate)) {
						score += 10;
						matched.add(preference);
					}
				}
			}
		}
		if (isWet(evidence) && value(candidate.walkingDurationSeconds()) >= 600) {
			score -= 15;
			risks.add("비·눈 상황에서 도보 노출시간이 깁니다.");
		}
		return new EvaluatedCandidate(candidate, Math.max(0, Math.min(100, score)), insights, risks, matched);
	}

	private double rewardMinimum(MobilityCandidate current, List<MobilityCandidate> all,
		Function<MobilityCandidate, Integer> extractor, double reward, String insight,
		List<String> insights, List<DecisionPreference> matched, DecisionPreference preference) {
		Integer currentValue = extractor.apply(current);
		if (currentValue == null) return 0;
		int minimum = all.stream().map(extractor).filter(java.util.Objects::nonNull)
			.mapToInt(Integer::intValue).min().orElse(Integer.MAX_VALUE);
		if (currentValue == minimum) {
			insights.add(insight);
			matched.add(preference);
			return reward;
		}
		return 0;
	}

	private boolean isRoadMode(MobilityCandidate value) {
		return value.transportType() == MobilityCandidate.TransportType.CAR
			|| value.transportType() == MobilityCandidate.TransportType.BUS
			|| value.transportType() == MobilityCandidate.TransportType.MIXED_TRANSIT;
	}

	private boolean isHeavyTraffic(CitySituation evidence) {
		String level = evidence.roadTraffic().level();
		return level != null && (level.contains("정체") || level.contains("서행"));
	}

	private boolean isWet(CitySituation evidence) {
		String condition = evidence.weather().condition();
		String precipitation = evidence.weather().precipitationType();
		return (condition != null && (condition.contains("비") || condition.contains("눈")))
			|| (precipitation != null && !precipitation.contains("없음") && !"0".equals(precipitation));
	}

	private int value(Integer value) { return value == null ? 0 : value; }
}
