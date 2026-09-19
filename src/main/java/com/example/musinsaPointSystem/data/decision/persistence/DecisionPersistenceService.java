package com.example.musinsaPointSystem.data.decision.persistence;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.musinsaPointSystem.data.decision.model.DecisionPreference;
import com.example.musinsaPointSystem.data.decision.model.DecisionRecommendation;
import com.example.musinsaPointSystem.data.decision.model.EvaluatedCandidate;
import com.example.musinsaPointSystem.data.location.model.Location;
import com.example.musinsaPointSystem.dto.mobility.CitySituation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DecisionPersistenceService {
	private final DecisionCaseJpaRepository repository;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	public DecisionPersistenceService(DecisionCaseJpaRepository repository, ObjectMapper objectMapper, Clock clock) {
		this.repository = repository; this.objectMapper = objectMapper; this.clock = clock;
	}

	@Transactional
	public String save(String userEmail, Location origin, Location destination,
		List<DecisionPreference> preferences, CitySituation evidence, List<EvaluatedCandidate> candidates,
		DecisionRecommendation recommendation) {
		String id = UUID.randomUUID().toString();
		String ids = candidates.stream().map(value -> value.candidate().candidateId())
			.collect(java.util.stream.Collectors.joining(","));
		repository.save(new DecisionCaseEntity(id, userEmail, json(origin), json(destination), json(preferences),
			json(evidence), json(candidates), ids, json(recommendation), recommendation.recommendedCandidateId(),
			OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)));
		return id;
	}

	@Transactional
	public SelectionResult select(String decisionId, String candidateId, String userEmail) {
		DecisionCaseEntity entity = repository.findById(decisionId)
			.orElseThrow(() -> new IllegalArgumentException("이동 결정 기록을 찾을 수 없습니다."));
		if (entity.getUserEmail() != null && !entity.getUserEmail().equals(userEmail))
			throw new IllegalArgumentException("이동 결정 기록에 접근할 수 없습니다.");
		boolean exists = Arrays.asList(entity.getCandidateIds().split(",")).contains(candidateId);
		if (!exists) throw new IllegalArgumentException("선택한 이동 후보가 존재하지 않습니다.");
		entity.select(candidateId, OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
		return new SelectionResult(entity.getId(), entity.getRecommendedCandidateId(),
			entity.getSelectedCandidateId(), entity.getStatus());
	}

	private String json(Object value) {
		try { return objectMapper.writeValueAsString(value); }
		catch (JsonProcessingException e) { throw new IllegalStateException("Decision snapshot serialization failed", e); }
	}

	public record SelectionResult(String decisionId, String recommendedCandidateId,
		String selectedCandidateId, String status) {}
}
