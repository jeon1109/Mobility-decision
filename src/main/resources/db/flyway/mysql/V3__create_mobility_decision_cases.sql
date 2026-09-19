CREATE TABLE mobility_decision_cases (
    id VARCHAR(36) PRIMARY KEY,
    user_email VARCHAR(255),
    origin_snapshot TEXT NOT NULL,
    destination_snapshot TEXT NOT NULL,
    preference_snapshot TEXT NOT NULL,
    evidence_snapshot TEXT NOT NULL,
    candidate_snapshot TEXT NOT NULL,
    candidate_ids TEXT NOT NULL,
    recommendation_snapshot TEXT NOT NULL,
    recommended_candidate_id VARCHAR(100),
    selected_candidate_id VARCHAR(100),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    decided_at TIMESTAMP NULL
) ENGINE=InnoDB;

CREATE INDEX idx_mobility_decision_user_created
    ON mobility_decision_cases (user_email, created_at);
