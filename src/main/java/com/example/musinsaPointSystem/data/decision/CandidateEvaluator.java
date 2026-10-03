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
import com.example.musinsaPointSystem.dto.mobility.CurrentEvidenceResponse;
import com.example.musinsaPointSystem.data.evidence.mobility.*;
import com.example.musinsaPointSystem.data.evidence.FreshnessStatus;

@Component
public class CandidateEvaluator {
	public List<EvaluatedCandidate> evaluate(List<MobilityCandidate> candidates,
		List<DecisionPreference> preferences, CitySituation evidence) {
		if (candidates == null || candidates.isEmpty()) return List.of();
		return candidates.stream().map(candidate -> evaluate(candidate, candidates, preferences, evidence, true))
			.sorted(Comparator.comparingDouble(EvaluatedCandidate::score).reversed()).toList();
	}

	private EvaluatedCandidate evaluate(MobilityCandidate candidate, List<MobilityCandidate> all,
		List<DecisionPreference> preferences, CitySituation evidence, boolean legacyRoadScoring) {
		double score = 50.0;
		List<String> insights = new ArrayList<>();
		List<String> risks = new ArrayList<>();
		List<DecisionPreference> matched = new ArrayList<>();

		for (DecisionPreference preference : preferences) {
			double before = score;
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
					if (legacyRoadScoring && isRoadMode(candidate) && isHeavyTraffic(evidence)) {
						score -= 22;
						risks.add("현재 도로 정체의 영향을 받을 수 있습니다.");
					} else if (legacyRoadScoring && !isRoadMode(candidate)) {
						score += 10;
						matched.add(preference);
					}
				}
			}
			// Preserve legacy multi-preference scoring; ranked UI requests contain one or two values.
			if (preferences.size() == 2 && preference == preferences.get(1)) score = before + (score - before) * 0.5;
		}
		if (isWet(evidence) && value(candidate.walkingDurationSeconds()) >= 600) {
			score -= 15;
			risks.add("비·눈 상황에서 도보 노출시간이 깁니다.");
		}
		return new EvaluatedCandidate(candidate, Math.max(0, Math.min(100, score)), insights, risks, matched);
	}

	public List<EvaluatedCandidate> evaluate(List<MobilityCandidate> candidates,
		List<DecisionPreference> preferences, CurrentEvidenceResponse evidence) {
		if (candidates == null || candidates.isEmpty()) return List.of();
		return candidates.stream().map(candidate -> {
			var base = evaluate(candidate, candidates, preferences, evidence.city(), false);
			if (!isRoadMode(candidate) || candidate.routeLinkIds().isEmpty()) return base;
			List<String> risks = new ArrayList<>(base.risks());
			double penalty = 0;
			int rank = preferences.indexOf(DecisionPreference.AVOID_CONGESTION);
			double weight = rank == 0 ? 1.0 : rank == 1 ? 0.5 : 0.25;
			boolean congestion = evidence.trafficAvailability() == EvidenceAvailability.AVAILABLE
				&& evidence.traffic().stream().anyMatch(t -> candidate.routeLinkIds().contains(t.linkId())
					&& t.freshness() == FreshnessStatus.FRESH && t.observedAt() != null
					&& (t.trafficStatus() == TrafficEvidence.TrafficStatus.CONGESTED
						|| t.trafficStatus() == TrafficEvidence.TrafficStatus.SLOW));
			if (congestion) { risks.add("TRAVEL_TIME_VARIABILITY: 경로 링크의 도로 혼잡이 확인되었습니다."); penalty += 22 * weight; }
			if (evidence.incidentAvailability() != EvidenceAvailability.UNAVAILABLE) {
				for (var incident : evidence.incidents()) {
					if (incident.linkId() == null || !candidate.routeLinkIds().contains(incident.linkId())
						|| incident.status() == IncidentEvidence.IncidentStatus.RESOLVED) continue;
					if (incident.status() == IncidentEvidence.IncidentStatus.ACTIVE
						&& incident.freshness() == FreshnessStatus.FRESH && incident.observedAt() != null
						&& evidence.incidentAvailability() == EvidenceAvailability.AVAILABLE
						&& incident.impacts().contains(IncidentEvidence.MobilityImpactType.ROAD_CONTROL)) {
						risks.add("ROAD_CONTROL_RISK: 경로 링크의 현재 도로 통제가 확인되었습니다."); penalty += 15 * weight;
					} else {
						risks.add("경로 링크에 돌발 기록이 있으나 현재 영향은 확인되지 않았습니다.");
					}
				}
			}
			return new EvaluatedCandidate(candidate, Math.max(0, base.score() - penalty), base.insights(), risks, base.matchedPreferences());
		}).sorted(Comparator.comparingDouble(EvaluatedCandidate::score).reversed()).toList();
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
