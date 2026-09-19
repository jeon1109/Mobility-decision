package com.example.musinsaPointSystem.data.decision.persistence;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mobility_decision_cases")
public class DecisionCaseEntity {
	@Id
	@Column(length = 36)
	private String id;
	@Column(name = "user_email", length = 255)
	private String userEmail;
	@Column(name = "origin_snapshot", nullable = false, columnDefinition = "TEXT")
	private String originSnapshot;
	@Column(name = "destination_snapshot", nullable = false, columnDefinition = "TEXT")
	private String destinationSnapshot;
	@Column(name = "preference_snapshot", nullable = false, columnDefinition = "TEXT")
	private String preferenceSnapshot;
	@Column(name = "evidence_snapshot", nullable = false, columnDefinition = "TEXT")
	private String evidenceSnapshot;
	@Column(name = "candidate_snapshot", nullable = false, columnDefinition = "TEXT")
	private String candidateSnapshot;
	@Column(name = "candidate_ids", nullable = false, columnDefinition = "TEXT")
	private String candidateIds;
	@Column(name = "recommendation_snapshot", nullable = false, columnDefinition = "TEXT")
	private String recommendationSnapshot;
	@Column(name = "recommended_candidate_id", length = 100)
	private String recommendedCandidateId;
	@Column(name = "selected_candidate_id", length = 100)
	private String selectedCandidateId;
	@Column(nullable = false, length = 32)
	private String status;
	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;
	@Column(name = "decided_at")
	private OffsetDateTime decidedAt;

	protected DecisionCaseEntity() {}

	public DecisionCaseEntity(String id, String userEmail, String originSnapshot, String destinationSnapshot,
		String preferenceSnapshot, String evidenceSnapshot, String candidateSnapshot, String candidateIds,
		String recommendationSnapshot, String recommendedCandidateId, OffsetDateTime createdAt) {
		this.id = id; this.userEmail = userEmail; this.originSnapshot = originSnapshot;
		this.destinationSnapshot = destinationSnapshot; this.preferenceSnapshot = preferenceSnapshot;
		this.evidenceSnapshot = evidenceSnapshot; this.candidateSnapshot = candidateSnapshot;
		this.candidateIds = candidateIds; this.recommendationSnapshot = recommendationSnapshot;
		this.recommendedCandidateId = recommendedCandidateId; this.status = "RECOMMENDED";
		this.createdAt = createdAt;
	}

	public String getId() { return id; }
	public String getUserEmail() { return userEmail; }
	public String getCandidateIds() { return candidateIds; }
	public String getRecommendedCandidateId() { return recommendedCandidateId; }
	public String getSelectedCandidateId() { return selectedCandidateId; }
	public String getStatus() { return status; }

	public void select(String candidateId, OffsetDateTime decidedAt) {
		this.selectedCandidateId = candidateId;
		this.decidedAt = decidedAt;
		this.status = "USER_DECIDED";
	}
}
