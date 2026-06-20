package com.example.musinsaPointSystem.message.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * Qmessage is a Querydsl query type for message
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class Qmessage extends EntityPathBase<message> {

    private static final long serialVersionUID = 1051442975L;

    public static final Qmessage message = new Qmessage("message");

    public final com.example.musinsaPointSystem.common.config.QBaseEntity _super = new com.example.musinsaPointSystem.common.config.QBaseEntity(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath nickname = createString("nickname");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    //inherited
    public final StringPath updatedBy = _super.updatedBy;

    public final StringPath userId = createString("userId");

    public Qmessage(String variable) {
        super(message.class, forVariable(variable));
    }

    public Qmessage(Path<? extends message> path) {
        super(path.getType(), path.getMetadata());
    }

    public Qmessage(PathMetadata metadata) {
        super(message.class, metadata);
    }

}

