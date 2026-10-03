package com.example.musinsaPointSystem.data.decision.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionCaseJpaRepository extends JpaRepository<DecisionCaseEntity, String> {
	@Override
	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	java.util.Optional<DecisionCaseEntity> findById(String id);
}
