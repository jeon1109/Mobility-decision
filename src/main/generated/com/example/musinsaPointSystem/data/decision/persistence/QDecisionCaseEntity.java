package com.example.musinsaPointSystem.data.decision.persistence;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDecisionCaseEntity is a Querydsl query type for DecisionCaseEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDecisionCaseEntity extends EntityPathBase<DecisionCaseEntity> {

    private static final long serialVersionUID = 26943080L;

    public static final QDecisionCaseEntity decisionCaseEntity = new QDecisionCaseEntity("decisionCaseEntity");

    public final StringPath candidateIds = createString("candidateIds");

    public final StringPath candidateSnapshot = createString("candidateSnapshot");

    public final DateTimePath<java.time.OffsetDateTime> createdAt = createDateTime("createdAt", java.time.OffsetDateTime.class);

    public final DateTimePath<java.time.OffsetDateTime> decidedAt = createDateTime("decidedAt", java.time.OffsetDateTime.class);

    public final StringPath destinationSnapshot = createString("destinationSnapshot");

    public final StringPath evidenceSnapshot = createString("evidenceSnapshot");

    public final StringPath id = createString("id");

    public final StringPath originSnapshot = createString("originSnapshot");

    public final StringPath preferenceSnapshot = createString("preferenceSnapshot");

    public final StringPath recommendationSnapshot = createString("recommendationSnapshot");

    public final StringPath recommendedCandidateId = createString("recommendedCandidateId");

    public final StringPath selectedCandidateId = createString("selectedCandidateId");

    public final StringPath status = createString("status");

    public final StringPath userEmail = createString("userEmail");

    public QDecisionCaseEntity(String variable) {
        super(DecisionCaseEntity.class, forVariable(variable));
    }

    public QDecisionCaseEntity(Path<? extends DecisionCaseEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDecisionCaseEntity(PathMetadata metadata) {
        super(DecisionCaseEntity.class, metadata);
    }

}

